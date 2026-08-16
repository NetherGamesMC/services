package org.nethergames.gsms.domain.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.nethergames.gsms.domain.dto.CreateGameServerStateDTO;
import org.nethergames.gsms.domain.dto.GameServerStateListDTO;
import org.nethergames.gsms.domain.dto.UpdateGameServerStateDTO;
import org.nethergames.gsms.domain.model.GameServerState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Mapper(componentModel = "spring")
public interface GameServerStateMapper {

	@Mapping(target = "metadata", expression = "java(org.nethergames.common.domain.util.ServerIdUtil.fromString(dto.getServerId()))")
	GameServerState toEntity(CreateGameServerStateDTO dto);

	void updateEntity(UpdateGameServerStateDTO dto, @MappingTarget GameServerState entity);

	GameServerStateListDTO toListViewDTO(GameServerState entity);

	default List<GameServerState> toEntities(Iterable<CreateGameServerStateDTO> dtos) {
		if (null == dtos) return new ArrayList<>();
		return StreamSupport.stream(dtos.spliterator(), false)
				.map(this::toEntity)
				.collect(Collectors.toList());
	}

	default void updateEntities(Iterable<UpdateGameServerStateDTO> dtos, Iterable<GameServerState> entities) {
		if (null == dtos) return;

		for (UpdateGameServerStateDTO dto : dtos) {
			Optional<GameServerState> opt = StreamSupport.stream(entities.spliterator(), false)
					.filter(t -> t.getId().equals(dto.getId()))
					.findFirst();
			opt.ifPresent(GameServerState -> updateEntity(dto, GameServerState));
		}
	}

	default List<GameServerStateListDTO> toListViewDTOs(Iterable<GameServerState> entities) {
		if (null == entities) return new ArrayList<>();
		return StreamSupport.stream(entities.spliterator(), false)
				.map(this::toListViewDTO)
				.collect(Collectors.toList());
	}

}
