package org.nethergames.gsms.domain.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.nethergames.common.domain.model.values.GameServerMetadata;
import org.nethergames.gsms.domain.dto.CreateGameServerDTO;
import org.nethergames.gsms.domain.dto.GameServerListDTO;
import org.nethergames.gsms.domain.dto.UpdateGameServerDTO;
import org.nethergames.gsms.domain.model.GameServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Mapper(componentModel = "spring")
public interface GameServerMapper {

	@Mapping(target = "metadata", expression = "java(org.nethergames.common.domain.util.ServerIdUtil.fromString(dto.getServerId()))")
	GameServer toEntity(CreateGameServerDTO dto);

	void updateEntity(UpdateGameServerDTO dto, @MappingTarget GameServer entity);

	GameServerListDTO toListViewDTO(GameServer entity);

	default List<GameServer> toEntities(Iterable<CreateGameServerDTO> dtos) {
		if (null == dtos) return new ArrayList<>();
		return StreamSupport.stream(dtos.spliterator(), false)
				.map(this::toEntity)
				.collect(Collectors.toList());
	}

	default void updateEntities(Iterable<UpdateGameServerDTO> dtos, Iterable<GameServer> entities) {
		if (null == dtos) return;

		for (UpdateGameServerDTO dto : dtos) {
			Optional<GameServer> opt = StreamSupport.stream(entities.spliterator(), false)
					.filter(t -> t.getId().equals(dto.getId()))
					.findFirst();
			opt.ifPresent(gameServer -> {
				updateEntity(dto, gameServer);
			});
		}
	}

	default List<GameServerListDTO> toListViewDTOs(Iterable<GameServer> entities) {
		if (null == entities) return new ArrayList<>();
		return StreamSupport.stream(entities.spliterator(), false)
				.map(this::toListViewDTO)
				.collect(Collectors.toList());
	}

}
