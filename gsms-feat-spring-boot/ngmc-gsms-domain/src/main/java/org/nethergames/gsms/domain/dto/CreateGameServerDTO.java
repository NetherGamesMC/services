package org.nethergames.gsms.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nethergames.common.domain.dto.BaseCreationDTO;
import org.nethergames.common.domain.model.IEntityDTO;
import org.nethergames.gsms.domain.constants.ServerMode;
import org.nethergames.gsms.domain.constants.ServerStatus;
import org.nethergames.gsms.domain.model.values.NetworkEndpoint;
import org.nethergames.gsms.domain.model.values.NetworkProtocol;

import java.util.Set;
import java.util.UUID;

@Schema(description = "DTO for creating a new game server")
public class CreateGameServerDTO extends BaseCreationDTO implements IEntityDTO<UUID> {

	@NotBlank(message = "Id must not be blank")
	@Size(max = 24, message = "Id must be between 1 to 24 characters")
	@Schema(description = "Id", requiredMode = Schema.RequiredMode.REQUIRED)
	private UUID id;

	@NotBlank(message = "Server ID cannot be blank")
	@Schema(description = "Kubernetes server unique identifier")
	private String serverId;

	@NotNull(message = "Connected status cannot be null")
	@Schema(description = "Whether the server is currently connected to the game server manager")
	private Boolean connected;

	@Schema(description = "Game server mode, used to identify servers for deterministic queuing")
	private ServerMode mode;

	@Schema(description = "The server status")
	private ServerStatus status;

	@Schema(description = "An object representing the server's network endpoint")
	private NetworkEndpoint endpoint;

	@Schema(description = "A set of network protocols for the server")
	private Set<NetworkProtocol> protocols;

	@Override
	public boolean isNew() {
		return null == this.getId();
	}

	@Override
	public UUID getId() {
		return this.id;
	}

	@Override
	public void setId(UUID id) {
		this.id = id;
	}

	public String getServerId() {
		return serverId;
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	public Boolean getConnected() {
		return connected;
	}

	public void setConnected(Boolean connected) {
		this.connected = connected;
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

	@Override
	public Long getVersion() {
		return null;
	}

	@Override
	public void setVersion(Long version) {}
}
