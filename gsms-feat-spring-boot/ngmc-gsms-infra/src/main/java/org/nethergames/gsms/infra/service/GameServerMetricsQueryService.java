package org.nethergames.gsms.infra.service;

import org.nethergames.gsms.domain.dto.GameServerMetricsListDTO;
import org.nethergames.gsms.domain.model.filter.GameServerMetricsFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GameServerMetricsQueryService {

	List<GameServerMetricsListDTO> findAll(Iterable<String> ids);

	Page<GameServerMetricsListDTO> findAll(GameServerMetricsFilter filter, Pageable pageable);
}
