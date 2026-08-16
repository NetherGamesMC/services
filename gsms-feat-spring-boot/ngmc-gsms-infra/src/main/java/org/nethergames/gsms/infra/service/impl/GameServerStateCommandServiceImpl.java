package org.nethergames.gsms.infra.service.impl;

import org.nethergames.gsms.domain.dto.CreateGameServerStateDTO;
import org.nethergames.gsms.domain.dto.GameServerStateListDTO;
import org.nethergames.gsms.domain.dto.UpdateGameServerStateDTO;
import org.nethergames.gsms.domain.mapper.GameServerStateMapper;
import org.nethergames.gsms.domain.model.GameServerState;
import org.nethergames.gsms.domain.repository.GameServerStateRepository;
import org.nethergames.gsms.infra.service.GameServerStateCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class GameServerStateCommandServiceImpl implements GameServerStateCommandService {

	private final GameServerStateRepository repo;
	private final GameServerStateMapper mapper;

	public GameServerStateCommandServiceImpl(GameServerStateRepository repo, GameServerStateMapper mapper) {
		this.repo = repo;
		this.mapper = mapper;
	}

	@Override
	@Transactional
	public List<GameServerStateListDTO> addAll(Iterable<CreateGameServerStateDTO> dtos) {
		List<GameServerState> entities = mapper.toEntities(dtos);
		entities = repo.insert(entities);
		return mapper.toListViewDTOs(entities);
	}

	@Override
	@Transactional
	public List<GameServerStateListDTO> updateAll(Iterable<UpdateGameServerStateDTO> dtos) {
		Set<UUID> ids = StreamSupport.stream(dtos.spliterator(), false)
				.map(UpdateGameServerStateDTO::getId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		List<GameServerState> entities = repo.findAllById(ids);
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

		List<GameServerState> entities = repo.findAllById(uuids);
		repo.deleteAll(entities);
		return entities.size();
	}

}
