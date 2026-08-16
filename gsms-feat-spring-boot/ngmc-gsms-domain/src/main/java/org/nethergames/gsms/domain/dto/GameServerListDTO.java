package org.nethergames.gsms.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.nethergames.common.domain.dto.BaseEntityDTO;
import org.nethergames.common.domain.model.values.GameServerMetadata;
import org.nethergames.gsms.domain.constants.ServerMode;
import org.nethergames.gsms.domain.constants.ServerStatus;
import org.nethergames.gsms.domain.model.values.NetworkEndpoint;
import org.nethergames.gsms.domain.model.values.NetworkProtocol;

import java.util.Set;
import java.util.UUID;

@Schema(description = "Game server default domain transfer object")
public class GameServerListDTO extends BaseEntityDTO<UUID> {

	@Schema(description = "Unique identifier for the game server")
	private String serverId;

	@Schema(description = "The metadata object for the server unique id")
	private GameServerMetadata metadata;

	@Schema(description = "The mode of the server is running on")
	private ServerMode mode;

	@Schema(description = "The status of the server")
	private ServerStatus status;

	@Schema(description = "The server network details")
	private NetworkEndpoint endpoint;

	@Schema(description = "A set of network protocols for the server")
	private Set<NetworkProtocol> protocols;

	@Schema(description = "The connection status for the server")
	private Boolean connected;

	public String getServerId() {
		return serverId;
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	public GameServerMetadata getMetadata() {
		return metadata;
	}

	public void setMetadata(GameServerMetadata metadata) {
		this.metadata = metadata;
	}

	public ServerMode getMode() {
		return mode;
	}

	public void setMode(ServerMode mode) {
		this.mode = mode;
	}

	public ServerStatus getStatus() {
		return status;
	}

	public void setStatus(ServerStatus status) {
		this.status = status;
	}

	public NetworkEndpoint getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(NetworkEndpoint endpoint) {
		this.endpoint = endpoint;
	}

	public Set<NetworkProtocol> getProtocols() {
		return protocols;
	}

	public void setProtocols(Set<NetworkProtocol> protocols) {
		this.protocols = protocols;
	}

	public Boolean getConnected() {
		return connected;
	}

	public void setConnected(Boolean connected) {
		this.connected = connected;
	}
}
