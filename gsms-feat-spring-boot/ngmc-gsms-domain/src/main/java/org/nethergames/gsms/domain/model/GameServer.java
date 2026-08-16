package org.nethergames.gsms.domain.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.nethergames.common.domain.model.BaseEntity;
import org.nethergames.common.domain.model.IVersionable;
import org.nethergames.common.domain.model.values.GameServerMetadata;
import org.nethergames.common.domain.util.ServerIdUtil;
import org.nethergames.gsms.domain.constants.ServerMode;
import org.nethergames.gsms.domain.constants.ServerStatus;
import org.nethergames.gsms.domain.model.values.NetworkEndpoint;
import org.nethergames.gsms.domain.model.values.NetworkProtocol;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Document(GameServer.COLL_NAME)
public class GameServer extends BaseEntity<UUID> implements IVersionable {

	public static final @Transient String COLL_NAME = "gs_game_servers";

	@NotNull(message = "Server ID cannot be null")
	@Pattern(regexp = ServerIdUtil.kubeV1UIDString, message = "Server ID must be in format 'region-serverType-gameType-deploymentId-replicaId'")
	private String serverId;

	@Valid
	@NotNull(message = "Game server metadata cannot be null")
	private GameServerMetadata metadata;

	@NotNull(message = "Server mode cannot be null")
	private ServerMode mode;

	@NotNull(message = "Server status cannot be null")
	private ServerStatus status;

	@Valid
	private NetworkEndpoint endpoint;

	private Set<@Valid NetworkProtocol> protocols;

	@NotNull(message = "Connected status cannot be null")
	private Boolean connected;

	@CreatedDate
	private Instant createdAt;

	@Version
	private Long version;

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

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public Long getVersion() {
		return version;
	}

	@Override
	public void setVersion(Long version) {
		this.version = version;
	}

	public boolean hasChanged(GameServer previous) {
		if (previous == null) {
			return true;
		}

		return !Objects.equals(this.mode, previous.getMode()) ||
				!Objects.equals(this.status, previous.getStatus()) ||
				!Objects.equals(this.endpoint, previous.getEndpoint()) ||
				!Objects.equals(this.protocols, previous.getProtocols()) ||
				!Objects.equals(this.connected, previous.getConnected());
	}
}
