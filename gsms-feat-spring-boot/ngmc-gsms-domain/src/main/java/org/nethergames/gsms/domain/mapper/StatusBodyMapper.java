package org.nethergames.gsms.domain.mapper;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.nethergames.gsms.domain.dto.CreateGameServerStateDTO;
import org.nethergames.gsms.domain.dto.GameServerListDTO;
import org.nethergames.gsms.domain.dto.UpdateGameServerDTO;
import org.nethergames.gsms.domain.dto.UpdateGameServerStateDTO;
import org.nethergames.gsms.domain.model.values.NetworkProtocol;
import org.nethergames.gsms.rpc.StatusBody;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Mapper(componentModel = "spring", uses = {ServerStatusMapper.class})
public interface StatusBodyMapper {

	@Mapping(target = "mode", source = "body.serverMode")
	@Mapping(target = "status", source = "body.status")
	UpdateGameServerDTO toUpdateGameServerDTO(GameServerListDTO server, StatusBody body);

	@Mapping(target = "lastTps", source = "state.tps")
	@Mapping(target = "lastUsage", source = "state.usage")
	@Mapping(target = "lastMemoryUsage", source = "state.memoryUsage")
	@Mapping(target = "lastPlayerSeenAt", expression = "java(java.time.Instant.now())")
	@Mapping(target = "version", ignore = true)
	CreateGameServerStateDTO toCreateGameStateDto(UUID id, String serverId, StatusBody state);

	@Mapping(target = "lastTps", source = "state.tps")
	@Mapping(target = "lastUsage", source = "state.usage")
	@Mapping(target = "lastMemoryUsage", source = "state.memoryUsage")
	@Mapping(target = "version", source = "version")
	UpdateGameServerStateDTO toUpdateGameStateDto(UUID id, String serverId, StatusBody state, Long version);

	@AfterMapping
	default void toUpdateGameServerDTO(StatusBody body, @MappingTarget UpdateGameServerDTO dto) {
		Set<NetworkProtocol> protocols = new LinkedHashSet<>();

		// Well, fallback to default, I guess?
		if (StringUtils.isNotEmpty(body.getProxyServerInfoType())) {
			protocols.add(new NetworkProtocol(19132, body.getProxyServerInfoType()));
		}

		if (CollectionUtils.isNotEmpty(body.getConnectionInfoList())) {
			body.getConnectionInfoList().stream()
					.map(i -> new NetworkProtocol(i.getPort(), i.getProxyInfoType()))
					.forEach(protocols::add);
		}

		dto.setProtocols(protocols);
	}
}
