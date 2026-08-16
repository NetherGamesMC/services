package org.nethergames.gsms.infra.service.impl;

import org.nethergames.gsms.domain.dto.ProxyServerListDTO;
import org.nethergames.gsms.domain.mapper.ProxyServerMapper;
import org.nethergames.gsms.domain.model.ProxyServer;
import org.nethergames.gsms.domain.model.filter.ProxyServerFilter;
import org.nethergames.gsms.domain.repository.ProxyServerRepository;
import org.nethergames.gsms.domain.specs.ProxyServerSpecification;
import org.nethergames.gsms.infra.service.ProxyServerQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
public class ProxyServerQueryServiceImpl implements ProxyServerQueryService {

	private final ProxyServerRepository repo;

	private final ProxyServerMapper mapper;

	public ProxyServerQueryServiceImpl(ProxyServerRepository repo, ProxyServerMapper mapper) {
		this.repo = repo;
		this.mapper = mapper;
	}

	@Override
	public List<ProxyServerListDTO> findAll(Iterable<String> ids) {
		List<UUID> uuids = StreamSupport.stream(ids.spliterator(), false)
				.map(UUID::fromString)
				.toList();

		List<ProxyServer> results = repo.findAllById(uuids);
		return mapper.toListViewDTOs(results);
	}

	@Override
	public Page<ProxyServerListDTO> findAll(ProxyServerFilter filter, Pageable pageable) {
		ProxyServerSpecification spec = new ProxyServerSpecification(filter);
		Page<ProxyServer> page = repo.findAll(spec, pageable);

		List<ProxyServerListDTO> dtos = mapper.toListViewDTOs(page.getContent());
		return PageableExecutionUtils.getPage(dtos, pageable, page::getTotalElements);
	}
}
