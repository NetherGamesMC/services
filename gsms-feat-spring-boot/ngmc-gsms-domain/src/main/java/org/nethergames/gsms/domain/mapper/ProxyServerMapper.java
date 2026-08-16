package org.nethergames.gsms.domain.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.nethergames.common.domain.constants.ServerRegion;
import org.nethergames.gsms.domain.dto.CreateProxyServerDTO;
import org.nethergames.gsms.domain.dto.ProxyServerListDTO;
import org.nethergames.gsms.domain.dto.UpdateProxyServerDTO;
import org.nethergames.gsms.domain.model.ProxyServer;
import org.nethergames.gsms.rpc.ProxyModel;
import org.nethergames.gsms.rpc.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Mapper(componentModel = "spring")
public interface ProxyServerMapper {

	ProxyServer toEntity(CreateProxyServerDTO dto);

	void updateEntity(UpdateProxyServerDTO dto, @MappingTarget ProxyServer entity);

	ProxyServerListDTO toListViewDTO(ProxyServer entity);

	default List<ProxyServer> toEntities(Iterable<CreateProxyServerDTO> dtos) {
		if (null == dtos) return new ArrayList<>();
		return StreamSupport.stream(dtos.spliterator(), false)
				.map(this::toEntity)
				.collect(Collectors.toList());
	}

	default void updateEntities(Iterable<UpdateProxyServerDTO> dtos, Iterable<ProxyServer> entities) {
		if (null == dtos) return;

		for (UpdateProxyServerDTO dto : dtos) {
			Optional<ProxyServer> opt = StreamSupport.stream(entities.spliterator(), false)
					.filter(t -> t.getId().equals(dto.getId()))
					.findFirst();
			opt.ifPresent(ProxyServer -> updateEntity(dto, ProxyServer));
		}
	}

	default List<ProxyServerListDTO> toListViewDTOs(Iterable<ProxyServer> entities) {
		if (null == entities) return new ArrayList<>();
		return StreamSupport.stream(entities.spliterator(), false)
				.map(this::toListViewDTO)
				.collect(Collectors.toList());
	}

	CreateProxyServerDTO toCreateProxyServerDto(ProxyModel response);

	default ServerRegion fromRegionBase(Region region) {
		return switch (region) {
			case AP -> ServerRegion.AP;
			case US -> ServerRegion.US;
			case EU -> ServerRegion.EU;
			default -> ServerRegion.IND;
		};
	}
}
