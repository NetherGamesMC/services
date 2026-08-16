package org.nethergames.gsms.infra.service;

import org.nethergames.gsms.domain.dto.GameServerClusterStateAggregateDTO;
import org.nethergames.gsms.domain.dto.GameServerStateListDTO;
import org.nethergames.gsms.domain.model.filter.GameServerStateFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GameServerStateQueryService {

	List<GameServerStateListDTO> findAll(Iterable<String> ids);

	Page<GameServerStateListDTO> findAll(GameServerStateFilter filter, Pageable pageable);

	GameServerClusterStateAggregateDTO findAllClusters(GameServerStateFilter filter, Pageable pageable);
}
