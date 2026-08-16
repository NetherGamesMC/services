package org.nethergames.gsms.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.nethergames.common.domain.dto.BaseEntityDTO;
import org.nethergames.common.domain.model.values.GameServerMetadata;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Game server state default domain transfer object")
public class GameServerStateListDTO extends BaseEntityDTO<UUID> {

	@Schema(description = "Game server unique identifier")
	private String serverId;

	@Schema(description = "Game server metadata")
	private GameServerMetadata metadata;

	@Schema(description = "Current player count")
	private Integer playerCount;

	@Schema(description = "Maximum player count")
	private Integer maxPlayerCount;

	@Schema(description = "Last player seen at timestamp")
	private Instant lastPlayerSeenAt;

	@Schema(description = "Queueing state")
	private Boolean queueingState;

	@Schema(description = "Touch only state")
	private Boolean touchOnlyState;

	@Schema(description = "Last TPS value")
	private Float lastTps;

	@Schema(description = "Last CPU usage value")
	private Float lastUsage;

	@Schema(description = "Last memory usage value")
	private Float lastMemoryUsage;

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
}
