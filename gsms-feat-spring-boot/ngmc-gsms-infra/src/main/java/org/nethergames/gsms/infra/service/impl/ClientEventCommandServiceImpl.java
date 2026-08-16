package org.nethergames.gsms.infra.service.impl;

import de.jkeylockmanager.manager.KeyLockManager;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.nethergames.common.domain.constants.GameType;
import org.nethergames.common.domain.constants.ServerType;
import org.nethergames.gsms.domain.context.ClientContext;
import org.nethergames.gsms.domain.dto.*;
import org.nethergames.gsms.domain.mapper.GameEventMapper;
import org.nethergames.gsms.domain.mapper.ProxyServerMapper;
import org.nethergames.gsms.domain.mapper.ServerModelMapper;
import org.nethergames.gsms.domain.mapper.StatusBodyMapper;
import org.nethergames.gsms.domain.model.GameServer;
import org.nethergames.gsms.domain.model.filter.GameServerFilter;
import org.nethergames.gsms.domain.model.filter.GameServerStateFilter;
import org.nethergames.gsms.infra.service.*;
import org.nethergames.gsms.rpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ClientEventCommandServiceImpl implements ClientEventCommandService {

	private static final Logger log = LoggerFactory.getLogger(ClientEventCommandServiceImpl.class);

	private static final String GAME_EVENTS_LOCK = "game-events";
	private static final String PROXY_EVENTS_LOCK = "proxy-events";

	private final ApplicationContext context;
	private final MongoTemplate mongoTemplate;

	private final ProxyServerCommandService proxyServerCommandService;
	private final ProxyServerQueryService proxyServerQueryService;

	private final GameServerQueryService queryService;
	private final GameServerCommandService commandService;

	private final GameServerStateCommandService stateCommandService;
	private final GameServerStateQueryService stateQueryService;

	private final StatusBodyMapper statusBodyMapper;
	private final ServerModelMapper serverModelMapper;
	private final GameEventMapper gameEventMapper;
	private final ProxyServerMapper proxyServerMapper;

	private final ConcurrentHashMap<UUID, StreamObserver<ServerEvent>> proxyEventSubscribers = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<UUID, StreamObserver<GameEvent>> eventSubscribers = new ConcurrentHashMap<>();

	private final KeyLockManager lockManager;

	public ClientEventCommandServiceImpl(
			ApplicationContext context,
			MongoTemplate mongoTemplate,
			ProxyServerCommandService proxyServerCommandService,
			ProxyServerQueryService proxyServerQueryService,
			GameServerQueryService queryService,
			GameServerCommandService commandService,
			GameServerStateCommandService stateCommandService,
			GameServerStateQueryService stateQueryService,
			StatusBodyMapper statusBodyMapper,
			ServerModelMapper serverModelMapper,
			GameEventMapper gameEventMapper,
			ProxyServerMapper proxyServerMapper,
			KeyLockManager lockManager) {

		this.context = context;
		this.mongoTemplate = mongoTemplate;
		this.proxyServerCommandService = proxyServerCommandService;
		this.proxyServerQueryService = proxyServerQueryService;
		this.queryService = queryService;
		this.commandService = commandService;
		this.stateCommandService = stateCommandService;
		this.stateQueryService = stateQueryService;
		this.statusBodyMapper = statusBodyMapper;
		this.serverModelMapper = serverModelMapper;
		this.gameEventMapper = gameEventMapper;
		this.proxyServerMapper = proxyServerMapper;
		this.lockManager = lockManager;
	}

	@Override
	@Transactional
	public boolean registerGameEventStreams(StreamObserver<GameEvent> event) {
		UUID serverId = ClientContext.CTX_SERVER_UUID.get();
		if (eventSubscribers.containsKey(serverId)) {
			return false;
		}
		// Compute if the key is absent, the function will make sure that anything executed in here
		// prevents other threads from modifying it.
		eventSubscribers.computeIfAbsent(serverId, (serverId0) -> {
			mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(serverId0)), Update.update("connected", true), GameServer.COLL_NAME);

			// ensure removal on cancel / completion
			if (event instanceof ServerCallStreamObserver<GameEvent> observer) {
				log.info("[{}] Server connected", serverId0);
				observer.setOnCancelHandler(() -> removeGameEventStreams(serverId0));
			}

			return lockManager.executeLocked(GAME_EVENTS_LOCK, () -> {
				GameServerClusterStateAggregateDTO allClusters = stateQueryService.findAllClusters(new GameServerStateFilter(), Pageable.unpaged());
				List<GameEvent> results = gameEventMapper.toGameEventEntities(allClusters.getResults().getContent());
				results.forEach(event::onNext);
				return event;
			});
		});
		return true;
	}

	@Override
	@Transactional
	public void removeGameEventStreams(UUID podUid) {
		lockManager.executeLocked(GAME_EVENTS_LOCK, () -> {
			mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(podUid)), Update.update("connected", false), GameServer.COLL_NAME);

			StreamObserver<GameEvent> observer = eventSubscribers.remove(podUid);
			if (observer != null) {
				log.info("[{}] Server disconnected", podUid);

				try {
					observer.onCompleted();
				} catch (Throwable ignored) {
				}
			}
		});
	}

	@Override
	public boolean registerProxyEventStreams(StreamObserver<ServerEvent> event) {
		UUID serverId = ClientContext.CTX_SERVER_UUID.get();
		if (proxyEventSubscribers.containsKey(serverId)) {
			return false;
		}
		// Compute if the key is absent, the function will make sure that anything executed in here
		// prevents other threads from modifying it.
		proxyEventSubscribers.computeIfAbsent(serverId, (serverId0) -> {
			// ensure removal on cancel / completion
			if (event instanceof ServerCallStreamObserver<ServerEvent> observer) {
				log.info("[{}] Proxy connected", serverId0);
				observer.setOnCancelHandler(() -> {
					ClientEventCommandServiceImpl service = context.getBean(ClientEventCommandServiceImpl.class);
					service.removeGameEventStreams(serverId0);
				});
			}

			return lockManager.executeLocked(PROXY_EVENTS_LOCK, () -> {
				GameServerFilter filter = new GameServerFilter();
				filter.setConnected(true);

				Map<UUID, GameServerListDTO> servers = queryService.findAll(filter, Pageable.unpaged())
						.getContent().stream()
						.collect(Collectors.toMap(GameServerListDTO::getId, Function.identity()));

				List<String> serverIds = servers.keySet().stream().map(UUID::toString).collect(Collectors.toList());
				List<GameServerStateListDTO> states = stateQueryService.findAll(serverIds);

				states.forEach(dto -> {
					GameServerListDTO server = servers.get(dto.getId());
					ServerEvent serverEvent = serverModelMapper.toEntity(server, dto, ServerEventType.SERVER_ADD);
					event.onNext(serverEvent);
				});

				return event;
			});
		});

		return true;
	}

	@Override
	public void removeProxyEventStreams(UUID podUid) {
		lockManager.executeLocked(PROXY_EVENTS_LOCK, () -> {
			StreamObserver<ServerEvent> observer = proxyEventSubscribers.remove(podUid);
			if (observer != null) {
				log.info("[{}] Proxy disconnected", podUid);

				try {
					observer.onCompleted();
				} catch (Throwable ignored) {
				}
			}
		});
	}

	@Override
	@Transactional
	public void onEventReceived(UpdateModel response) {
		UUID serverId = ClientContext.CTX_SERVER_UUID.get();

		StatusBody status = response.getStatus();

		List<GameServerListDTO> servers = queryService.findAll(List.of(serverId.toString()));
		GameServerListDTO server = servers.stream().findFirst().orElse(null);
		if (server == null) {
			return;
		}

		updateGameServer(server, status);
		updateGameServerState(server, status);
		addGameServerAnalytics(server, status);
	}

	@Override
	@Transactional
	public void onEventReceived(ProxyModel response) {
		UUID serverId = ClientContext.CTX_SERVER_UUID.get();

		ProxyServerListDTO server = proxyServerQueryService.findAll(List.of(serverId.toString()))
				.stream().findFirst().orElse(null);
		if (server == null) {
			CreateProxyServerDTO dto = proxyServerMapper.toCreateProxyServerDto(response);
		}
	}

	@Override
	public void broadcastProxyEvent(GameServerListDTO server, ServerEventType eventType) {
		GameServerStateListDTO state = stateQueryService.findAll(List.of(server.getId().toString()))
				.stream().findFirst().orElse(null);
		ServerEvent serverEvent = serverModelMapper.toEntity(server, state, eventType);

		lockManager.executeLocked(PROXY_EVENTS_LOCK, () -> {
			for (Map.Entry<UUID, StreamObserver<ServerEvent>> entry : proxyEventSubscribers.entrySet()) {
				StreamObserver<ServerEvent> observer = entry.getValue();
				try {
					observer.onNext(serverEvent);
				} catch (Throwable ignored) {
				}
			}
		});
	}

	@Override
	public void broadcastClusterEvent(GameType gameType, ServerType serverType) {
		GameServerStateFilter filter = new GameServerStateFilter();
		filter.setGameTypes(List.of(gameType));
		filter.setServerTypes(List.of(serverType));

		GameServerClusterStateAggregateDTO clusters = stateQueryService.findAllClusters(filter, Pageable.unpaged());
		List<GameEvent> events = gameEventMapper.toGameEventEntities(clusters.getResults().getContent());

		// Create default cluster data if the current cluster is empty.
		if (events.isEmpty()) {
			events.add(gameEventMapper.toEmptyGameEventEntity(gameType, serverType));
		}

		lockManager.executeLocked(GAME_EVENTS_LOCK, () -> {
			for (Map.Entry<UUID, StreamObserver<GameEvent>> entry : eventSubscribers.entrySet()) {
				StreamObserver<GameEvent> observer = entry.getValue();
				try {
					events.forEach(observer::onNext);
				} catch (Throwable ignored) {
				}
			}
		});
	}

	private void updateGameServerState(GameServerListDTO serverInfo, StatusBody status) {
		List<GameServerStateListDTO> servers = stateQueryService.findAll(List.of(serverInfo.getId().toString()));
		GameServerStateListDTO server = servers.stream().findFirst().orElse(null);

		if (server == null) {
			CreateGameServerStateDTO dto = statusBodyMapper.toCreateGameStateDto(serverInfo.getId(), serverInfo.getServerId(), status);
			stateCommandService.addAll(List.of(dto));
		} else {
			UpdateGameServerStateDTO dto = statusBodyMapper.toUpdateGameStateDto(serverInfo.getId(), serverInfo.getServerId(), status, server.getVersion());
			stateCommandService.updateAll(List.of(dto));
		}
	}

	private void updateGameServer(GameServerListDTO server, StatusBody status) {
		UpdateGameServerDTO dto = statusBodyMapper.toUpdateGameServerDTO(server, status);
		commandService.updateAll(List.of(dto));
	}

	private void addGameServerAnalytics(GameServerListDTO server, StatusBody status) {

	}
}
