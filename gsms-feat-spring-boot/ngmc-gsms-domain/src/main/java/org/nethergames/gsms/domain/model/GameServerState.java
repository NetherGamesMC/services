package org.nethergames.gsms.domain.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.nethergames.common.domain.model.BaseEntity;
import org.nethergames.common.domain.model.IVersionable;
import org.nethergames.common.domain.model.values.GameServerMetadata;
import org.nethergames.common.domain.util.ServerIdUtil;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(GameServerState.COLL_NAME)
public class GameServerState extends BaseEntity<UUID> implements IVersionable {

	public static final @Transient String COLL_NAME = "gs_game_server_states";

	@NotNull(message = "Server ID cannot be null")
	@Pattern(regexp = ServerIdUtil.kubeV1UIDString, message = "Server ID must be in format 'region-serverType-gameType-deploymentId-replicaId'")
	private String serverId;

	@NotNull(message = "Document must have a valid game server metadata")
	private GameServerMetadata metadata;

	private Integer playerCount;
	private Integer maxPlayerCount;

	private Instant lastPlayerSeenAt;

	private Boolean queueingState;
	private Boolean touchOnlyState;

	private Float lastTps;
	private Float lastUsage;
	private Float lastMemoryUsage;

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

	public Integer getPlayerCount() {
		return playerCount;
	}

	public void setPlayerCount(Integer playerCount) {
		this.playerCount = playerCount;
	}

	public Integer getMaxPlayerCount() {
		return maxPlayerCount;
	}

	public void setMaxPlayerCount(Integer maxPlayerCount) {
		this.maxPlayerCount = maxPlayerCount;
	}

	public Instant getLastPlayerSeenAt() {
		return lastPlayerSeenAt;
	}

	public void setLastPlayerSeenAt(Instant lastPlayerSeenAt) {
		this.lastPlayerSeenAt = lastPlayerSeenAt;
	}

	public Boolean getQueueingState() {
		return queueingState;
	}

	public void setQueueingState(Boolean queueingState) {
		this.queueingState = queueingState;
	}

	public Boolean getTouchOnlyState() {
		return touchOnlyState;
	}

	public void setTouchOnlyState(Boolean touchOnlyState) {
		this.touchOnlyState = touchOnlyState;
	}

	public Float getLastTps() {
		return lastTps;
	}

	public void setLastTps(Float lastTps) {
		this.lastTps = lastTps;
	}

	public Float getLastUsage() {
		return lastUsage;
	}

	public void setLastUsage(Float lastUsage) {
		this.lastUsage = lastUsage;
	}

	public Float getLastMemoryUsage() {
		return lastMemoryUsage;
	}

	public void setLastMemoryUsage(Float lastMemoryUsage) {
		this.lastMemoryUsage = lastMemoryUsage;
	}

	@Override
	public Long getVersion() {
		return version;
	}

	@Override
	public void setVersion(Long version) {
		this.version = version;
	}
}
