package org.nethergames.gsms.infra.service.impl;

import org.nethergames.common.domain.service.BaseDomainService;
import org.nethergames.gsms.domain.dto.CreateGameServerDTO;
import org.nethergames.gsms.domain.dto.GameServerListDTO;
import org.nethergames.gsms.domain.dto.UpdateGameServerDTO;
import org.nethergames.gsms.domain.mapper.GameServerMapper;
import org.nethergames.gsms.domain.model.GameServer;
import org.nethergames.gsms.domain.repository.GameServerRepository;
import org.nethergames.gsms.infra.service.GameServerCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class GameServerCommandServiceImpl extends BaseDomainService implements GameServerCommandService {

	private static final Logger log = LoggerFactory.getLogger(GameServerCommandServiceImpl.class);

	private final GameServerRepository repo;

	private final GameServerMapper mapper;

	public GameServerCommandServiceImpl(GameServerRepository repo, GameServerMapper mapper) {
		this.repo = repo;
		this.mapper = mapper;
	}

	@Override
	@Transactional
	public List<GameServerListDTO> addAll(Iterable<CreateGameServerDTO> dtos) {
		List<GameServer> entities = mapper.toEntities(dtos);
		entities = repo.insert(entities);
		return mapper.toListViewDTOs(entities);
	}

	@Override
	@Transactional
	public List<GameServerListDTO> updateAll(Iterable<UpdateGameServerDTO> dtos) {
		Set<UUID> ids = StreamSupport.stream(dtos.spliterator(), false)
				.map(UpdateGameServerDTO::getId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		List<GameServer> entities = repo.findAllById(ids);
		mapper.updateEntities(dtos, entities);
		entities = repo.saveAll(entities);
		return mapper.toListViewDTOs(entities);
	}

	@Override
	@Transactional
	public long deleteAll(Iterable<String> ids) {
		List<UUID> uuids = StreamSupport.stream(ids.spliterator(), false)
				.map(UUID::fromString)
				.toList();

		List<GameServer> entities = repo.findAllById(uuids);
		repo.deleteAll(entities);
		return entities.size();
	}

}
