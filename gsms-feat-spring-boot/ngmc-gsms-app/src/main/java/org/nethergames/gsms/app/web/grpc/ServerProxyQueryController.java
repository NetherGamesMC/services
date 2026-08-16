package org.nethergames.gsms.app.web.grpc;

import io.fabric8.kubernetes.api.model.Pod;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.nethergames.gsms.app.interceptor.GameInterceptor;
import org.nethergames.gsms.app.streams.PodChangeStreamObserver;
import org.nethergames.gsms.app.streams.ProxyChangeStreamObserver;
import org.nethergames.gsms.domain.constants.ServerCategory;
import org.nethergames.gsms.domain.context.ClientContext;
import org.nethergames.gsms.domain.dto.GameServerListDTO;
import org.nethergames.gsms.domain.model.filter.GameServerFilter;
import org.nethergames.gsms.infra.service.ClientEventCommandService;
import org.nethergames.gsms.infra.service.GameServerQueryService;
import org.nethergames.gsms.infra.service.KubernetesService;
import org.nethergames.gsms.rpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.grpc.server.service.GrpcService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@GrpcService(interceptors = GameInterceptor.class)
public class ServerProxyQueryController extends GSMSServiceGrpc.GSMSServiceImplBase {

	private static final Logger log = LoggerFactory.getLogger(ServerProxyQueryController.class);

	private final ClientEventCommandService commandService;

	private final KubernetesService kubernetesService;

	private final GameServerQueryService queryService;

	public ServerProxyQueryController(ClientEventCommandService commandService, KubernetesService kubernetesService, GameServerQueryService queryService) {
		this.commandService = commandService;
		this.kubernetesService = kubernetesService;
		this.queryService = queryService;
	}

	@Override
	public StreamObserver<UpdateModel> registerListenerGame(StreamObserver<GameEvent> responseObserver) {
		String serverId = ClientContext.CTX_SERVER_ID.get();
		ServerCategory category = ClientContext.CTX_SERVER_TYPE.get();

		if (category != ServerCategory.GAME) {
			responseObserver.onError(Status.ABORTED
					.withDescription("Remote server requested for game server procedural call while being a proxy")
					.asException());
			return null;
		}

		GameServerFilter filter = new GameServerFilter();
		filter.setServerIds(List.of(serverId));
		Page<GameServerListDTO> servers = queryService.findAll(filter, Pageable.unpaged());

		// If there are no such servers found with the given filters, then we reject the connection.
		if (servers.isEmpty()) {
			responseObserver.onError(Status.INVALID_ARGUMENT
					.withDescription("Kubernetes has not registered any servers with the id of " + serverId)
					.asException());
			return null;
		}

		GameServerListDTO server = servers.getContent().getFirst();

		// Register game event streams into local storage so update/changes can propagate
		AtomicBoolean hasRegistered = new AtomicBoolean(false);
		Context.current().withValue(ClientContext.CTX_SERVER_UUID, server.getId())
				.run(() -> hasRegistered.set(commandService.registerGameEventStreams(responseObserver)));
		if (!hasRegistered.get()) {
			responseObserver.onError(Status.ALREADY_EXISTS
					.withDescription("The server with id of " + serverId + " has already subscribed to this service")
					.asException());
			return null;
		}

		// Serve requests as usual
		return new PodChangeStreamObserver(commandService, server.getId());
	}

	@Override
	public StreamObserver<ProxyModel> registerListenerProxy(StreamObserver<ServerEvent> responseObserver) {
		String serverId = ClientContext.CTX_SERVER_ID.get();
		ServerCategory category = ClientContext.CTX_SERVER_TYPE.get();

		if (category != ServerCategory.PROXY) {
			responseObserver.onError(Status.ABORTED
					.withDescription("Remote server requested for proxy procedural call while being a game server")
					.asException());
			return null;
		}

		Pod pod = kubernetesService.getProxyFromName(serverId);
		if (pod == null || pod.getMetadata() == null) {
			responseObserver.onError(Status.INVALID_ARGUMENT
					.withDescription("Kubernetes has not registered any proxy with the id of " + serverId)
					.asException());
			return null;
		}

		UUID podUid = UUID.fromString(pod.getMetadata().getUid());

		// Register proxy event streams into local storage so update/changes can propagate
		AtomicBoolean hasRegistered = new AtomicBoolean(false);
		Context.current().withValue(ClientContext.CTX_SERVER_UUID, podUid)
				.run(() -> hasRegistered.set(commandService.registerProxyEventStreams(responseObserver)));
		if (!hasRegistered.get()) {
			responseObserver.onError(Status.ALREADY_EXISTS
					.withDescription("The server with id of " + serverId + " has already subscribed to this service")
					.asException());
			return null;
		}

		return new ProxyChangeStreamObserver(commandService, podUid);
	}
}
