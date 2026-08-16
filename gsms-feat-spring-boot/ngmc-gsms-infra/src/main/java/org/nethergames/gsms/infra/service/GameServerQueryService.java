package org.nethergames.gsms.infra.service;

import org.nethergames.gsms.domain.dto.GameServerListDTO;
import org.nethergames.gsms.domain.model.filter.GameServerFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GameServerQueryService {

	List<GameServerListDTO> findAll(Iterable<String> ids);

	Page<GameServerListDTO> findAll(GameServerFilter filter, Pageable pageable);
}
