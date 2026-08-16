package org.nethergames.gsms.domain.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.nethergames.common.domain.constants.GameType;
import org.nethergames.common.domain.constants.ServerType;
import org.nethergames.gsms.domain.dto.GameServerClusterStateListDTO;
import org.nethergames.gsms.domain.dto.values.GameServerClusterDataListDTO;
import org.nethergames.gsms.rpc.GameEvent;
import org.nethergames.gsms.rpc.GameServerStatus;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface GameEventMapper {

	@Mapping(target = "clusterDataBuilderList", ignore = true)
	@Mapping(target = "clusterDataList", ignore = true)
	@Mapping(target = "unknownFields", ignore = true)
	@Mapping(target = "serverTypeBytes", ignore = true)
	@Mapping(target = "removeClusterData", ignore = true)
	@Mapping(target = "mergeUnknownFields", ignore = true)
	@Mapping(target = "mergeFrom", ignore = true)
	@Mapping(target = "gameTypeBytes", ignore = true)
	@Mapping(target = "clusterDataOrBuilderList", ignore = true)
	@Mapping(target = "clearOneof", ignore = true)
	@Mapping(target = "clearField", ignore = true)
	@Mapping(target = "allFields", ignore = true)
	GameEvent toGameEventEntity(GameServerClusterStateListDTO clusterState);

	@Mapping(target = "serverUniqueId", source = "serverId")
	@Mapping(target = "unknownFields", ignore = true)
	@Mapping(target = "serverUniqueIdBytes", ignore = true)
	@Mapping(target = "mergeUnknownFields", ignore = true)
	@Mapping(target = "mergeFrom", ignore = true)
	@Mapping(target = "clearOneof", ignore = true)
	@Mapping(target = "clearField", ignore = true)
	@Mapping(target = "allFields", ignore = true)
	GameServerStatus toGameServerStatusEntity(GameServerClusterDataListDTO clusterState);

	default List<GameEvent> toGameEventEntities(List<GameServerClusterStateListDTO> clusterStates) {
		return clusterStates.stream()
				.map(this::toGameEventEntity)
				.collect(Collectors.toList());
	}

	default GameEvent toEmptyGameEventEntity(GameType gameType, ServerType serverType) {
		return GameEvent.newBuilder()
				.setGameType(gameType.name())
				.setServerType(serverType.name())
				.setMaxPlayers(0)
				.setTotalPlayers(0)
				.build();
	}

	@AfterMapping
	default void afterGameEventEntityMapping(GameServerClusterStateListDTO clusterState, @MappingTarget GameEvent.Builder entity) {
		clusterState.getClusterData().forEach(dto -> {
			GameServerStatus status = toGameServerStatusEntity(dto);
			entity.addClusterData(status);
		});
	}
}
