package org.nethergames.gsms.infra.service;

import org.nethergames.gsms.domain.dto.ProxyServerListDTO;
import org.nethergames.gsms.domain.model.filter.ProxyServerFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProxyServerQueryService {
	List<ProxyServerListDTO> findAll(Iterable<String> ids);

	Page<ProxyServerListDTO> findAll(ProxyServerFilter filter, Pageable pageable);
}
