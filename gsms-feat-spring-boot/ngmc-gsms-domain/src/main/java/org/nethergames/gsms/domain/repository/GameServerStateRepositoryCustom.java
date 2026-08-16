package org.nethergames.gsms.domain.repository;

import org.nethergames.gsms.domain.dto.GameServerClusterStateAggregateDTO;
import org.nethergames.gsms.domain.model.filter.GameServerStateFilter;
import org.springframework.data.domain.Pageable;

public interface GameServerStateRepositoryCustom {

	GameServerClusterStateAggregateDTO findAll(GameServerStateFilter filter, Pageable pageable);
}
