package org.nethergames.gsms.infra.service.impl;

import org.nethergames.gsms.domain.dto.GameServerClusterStateAggregateDTO;
import org.nethergames.gsms.domain.dto.GameServerStateListDTO;
import org.nethergames.gsms.domain.mapper.GameServerStateMapper;
import org.nethergames.gsms.domain.model.GameServerState;
import org.nethergames.gsms.domain.model.filter.GameServerStateFilter;
import org.nethergames.gsms.domain.repository.GameServerStateRepository;
import org.nethergames.gsms.domain.specs.GameServerStateSpecification;
import org.nethergames.gsms.infra.service.GameServerStateQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
public class GameServerStateQueryServiceImpl implements GameServerStateQueryService {

	private final GameServerStateRepository stateRepo;

	private final GameServerStateMapper stateMapper;

	public GameServerStateQueryServiceImpl(GameServerStateRepository stateRepo, GameServerStateMapper stateMapper) {
		this.stateRepo = stateRepo;
		this.stateMapper = stateMapper;
	}

	@Override
	public List<GameServerStateListDTO> findAll(Iterable<String> ids) {
		List<UUID> uuids = StreamSupport.stream(ids.spliterator(), false)
				.map(UUID::fromString)
				.toList();

		List<GameServerState> results = stateRepo.findAllById(uuids);
		return stateMapper.toListViewDTOs(results);
	}

	@Override
	public Page<GameServerStateListDTO> findAll(GameServerStateFilter filter, Pageable pageable) {
		GameServerStateSpecification spec = new GameServerStateSpecification(filter);
		Page<GameServerState> page = stateRepo.findAll(spec, pageable);

		List<GameServerStateListDTO> dtos = stateMapper.toListViewDTOs(page.getContent());
		return PageableExecutionUtils.getPage(dtos, pageable, page::getTotalElements);
	}

	@Override
	public GameServerClusterStateAggregateDTO findAllClusters(GameServerStateFilter filter, Pageable pageable){
		return stateRepo.findAll(filter, pageable);
	}
}
