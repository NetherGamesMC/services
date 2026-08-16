package org.nethergames.gsms.infra.service.impl;

import org.nethergames.gsms.domain.dto.CreateProxyServerDTO;
import org.nethergames.gsms.domain.dto.ProxyServerListDTO;
import org.nethergames.gsms.domain.dto.UpdateProxyServerDTO;
import org.nethergames.gsms.domain.mapper.ProxyServerMapper;
import org.nethergames.gsms.domain.model.ProxyServer;
import org.nethergames.gsms.domain.repository.ProxyServerRepository;
import org.nethergames.gsms.infra.service.ProxyServerCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class ProxyServerCommandServiceImpl implements ProxyServerCommandService {

	private final ProxyServerRepository repo;

	private final ProxyServerMapper mapper;

	public ProxyServerCommandServiceImpl(ProxyServerRepository repo, ProxyServerMapper mapper) {
		this.repo = repo;
		this.mapper = mapper;
	}

	@Override
	@Transactional
	public List<ProxyServerListDTO> addAll(Iterable<CreateProxyServerDTO> dtos) {
		List<ProxyServer> entities = mapper.toEntities(dtos);
		entities = repo.insert(entities);
		return mapper.toListViewDTOs(entities);
	}

	@Override
	@Transactional
	public List<ProxyServerListDTO> updateAll(Iterable<UpdateProxyServerDTO> dtos) {
		Set<UUID> ids = StreamSupport.stream(dtos.spliterator(), false)
				.map(UpdateProxyServerDTO::getId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		List<ProxyServer> entities = repo.findAllById(ids);
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

		List<ProxyServer> entities = repo.findAllById(uuids);
		repo.deleteAll(entities);
		return entities.size();
	}
}
