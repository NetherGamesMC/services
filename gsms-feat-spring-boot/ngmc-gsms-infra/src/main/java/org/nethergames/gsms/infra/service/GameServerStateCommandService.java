package org.nethergames.gsms.infra.service;

import org.nethergames.gsms.domain.dto.CreateGameServerStateDTO;
import org.nethergames.gsms.domain.dto.GameServerStateListDTO;
import org.nethergames.gsms.domain.dto.UpdateGameServerStateDTO;

import java.util.List;

public interface GameServerStateCommandService {

	List<GameServerStateListDTO> addAll(Iterable<CreateGameServerStateDTO> dtos);

	List<GameServerStateListDTO> updateAll(Iterable<UpdateGameServerStateDTO> dtos);

	long deleteAll(Iterable<String> ids);

}
