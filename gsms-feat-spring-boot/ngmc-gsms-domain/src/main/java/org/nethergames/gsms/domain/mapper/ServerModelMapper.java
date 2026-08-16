package org.nethergames.gsms.domain.mapper;

import org.apache.commons.collections4.CollectionUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.nethergames.common.domain.constants.ServerRegion;
import org.nethergames.gsms.domain.dto.GameServerListDTO;
import org.nethergames.gsms.domain.dto.GameServerStateListDTO;
import org.nethergames.gsms.domain.model.values.NetworkEndpoint;
import org.nethergames.gsms.domain.model.values.NetworkProtocol;
import org.nethergames.gsms.rpc.*;

import java.util.Set;

import static org.nethergames.gsms.rpc.ServerStatus.STATUS_ONLINE;
import static org.nethergames.gsms.rpc.ServerStatus.STATUS_TERMINATING;

@Mapper(componentModel = "spring")
public interface ServerModelMapper {

	@Mapping(target = "server", expression = "java(toEntityModel(gameServer, gameServerState))")
	ServerEvent toEntity(GameServerListDTO gameServer, GameServerStateListDTO gameServerState, ServerEventType eventType);

	@Mapping(target = "serverUniqueId", source = "gameServerState.serverId")
	@Mapping(target = "serverType", source = "gameServer.metadata.serverType")
	@Mapping(target = "gameType", source = "gameServer.metadata.gameType")
	@Mapping(target = "region", source = "gameServer.metadata.serverRegion")
	@Mapping(target = "status", source = "gameServer.status")
	ServerModel toEntityModel(GameServerListDTO gameServer, GameServerStateListDTO gameServerState);

	@AfterMapping
	default void afterServerModelEntityMapping(GameServerListDTO server, @MappingTarget ServerModel.Builder entity) {
		if (server != null) {
			networkMapping(server.getEndpoint(), server.getProtocols(), entity);
		}
	}

	default void networkMapping(NetworkEndpoint endpoint, Set<NetworkProtocol> protocols, @MappingTarget ServerModel.Builder entity) {
		NetworkProtocol protocol = CollectionUtils.isNotEmpty(protocols) ? protocols.stream().findFirst().orElse(null) : null;
		String serverInfo = protocol != null ? protocol.getProxyType() : "quic";
		int port = protocol != null ? protocol.getPort() : 19132;

		String serverIp = endpoint.getIp();
		entity.setAddress(serverIp);
		entity.setPort(port);
		entity.setProxyServerInfoType(serverInfo);

		if (CollectionUtils.isNotEmpty(protocols)) {
			protocols.stream().map(o -> ConnectionInfo.newBuilder()
					.setPort(o.getPort())
					.setProxyInfoType(o.getProxyType()).build()
			).forEach(entity::addConnectionInfo);
		} else {
			entity.addConnectionInfo(ConnectionInfo.newBuilder()
					.setPort(port)
					.setProxyInfoType(serverInfo)
					.build());
		}
	}

	default Region fromRegionBase(ServerRegion region) {
		return switch (region) {
			case AP -> Region.AP;
			case US -> Region.US;
			case EU -> Region.EU;
			default -> Region.IND;
		};
	}

	default ServerStatus toServerStatus(org.nethergames.gsms.domain.constants.ServerStatus statusValue) {
		return switch (statusValue) {
			case WAITING, RUNNING -> STATUS_ONLINE;
			case TERMINATING -> STATUS_TERMINATING;
		};
	}
}
