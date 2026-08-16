package org.nethergames.gsms.infra.service;

import org.nethergames.gsms.domain.dto.CreateGameServerDTO;
import org.nethergames.gsms.domain.dto.GameServerListDTO;
import org.nethergames.gsms.domain.dto.UpdateGameServerDTO;

import java.util.List;

public interface GameServerCommandService {
	List<GameServerListDTO> addAll(Iterable<CreateGameServerDTO> dtos);

	List<GameServerListDTO> updateAll(Iterable<UpdateGameServerDTO> dtos);

	long deleteAll(Iterable<String> ids);

}
