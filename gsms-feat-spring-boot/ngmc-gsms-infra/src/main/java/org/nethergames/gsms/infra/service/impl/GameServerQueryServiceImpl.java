package org.nethergames.gsms.infra.service.impl;

import org.nethergames.common.domain.service.BaseDomainService;
import org.nethergames.gsms.domain.dto.GameServerListDTO;
import org.nethergames.gsms.domain.mapper.GameServerMapper;
import org.nethergames.gsms.domain.model.GameServer;
import org.nethergames.gsms.domain.model.filter.GameServerFilter;
import org.nethergames.gsms.domain.repository.GameServerRepository;
import org.nethergames.gsms.domain.specs.GameServerSpecification;
import org.nethergames.gsms.infra.service.GameServerQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
public class GameServerQueryServiceImpl extends BaseDomainService implements GameServerQueryService {

	private final GameServerRepository repo;

	private final GameServerMapper mapper;

	public GameServerQueryServiceImpl(GameServerRepository repo, GameServerMapper mapper) {
		this.repo = repo;
		this.mapper = mapper;
	}

	@Override
	public List<GameServerListDTO> findAll(Iterable<String> ids) {
		List<UUID> uuids = StreamSupport.stream(ids.spliterator(), false)
				.map(UUID::fromString)
				.toList();

		List<GameServer> results = repo.findAllById(uuids);
		return mapper.toListViewDTOs(results);
	}

	@Override
	public Page<GameServerListDTO> findAll(GameServerFilter filter, Pageable pageable) {
		GameServerSpecification spec = new GameServerSpecification(filter);
		Page<GameServer> page = repo.findAll(spec, pageable);

		List<GameServerListDTO> dtos = mapper.toListViewDTOs(page.getContent());
		return PageableExecutionUtils.getPage(dtos, pageable, page::getTotalElements);
	}
}
