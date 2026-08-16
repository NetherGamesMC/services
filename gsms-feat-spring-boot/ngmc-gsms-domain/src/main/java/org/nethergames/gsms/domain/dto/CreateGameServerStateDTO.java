package org.nethergames.gsms.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.nethergames.common.domain.dto.BaseCreationDTO;
import org.nethergames.common.domain.model.IEntityDTO;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "DTO for creating a new game server state")
public class CreateGameServerStateDTO extends BaseCreationDTO implements IEntityDTO<UUID> {

	@NotBlank(message = "Id must not be blank")
	@Size(max = 24, message = "Id must be between 1 to 24 characters")
	@Schema(description = "Id", requiredMode = Schema.RequiredMode.REQUIRED)
	private UUID id;

	@NotBlank(message = "Server ID cannot be blank")
	@Schema(description = "Kubernetes server unique identifier")
	private String serverId;

	@Positive(message = "Player count must not be negative")
	@Schema(description = "Current player count")
	private Integer playerCount;

	@Positive(message = "Maximum player count must not be negative")
	@Schema(description = "Maximum player count")
	private Integer maxPlayerCount;

	@NotNull(message = "Last player seen at timestamp cannot be null")
	@Schema(description = "Last player seen at timestamp")
	private Instant lastPlayerSeenAt;

	@Schema(description = "Queueing state")
	private Boolean queueingState;

	@Schema(description = "Touch only state")
	private Boolean touchOnlyState;

	@PositiveOrZero(message = "Last server TPS must not be negative")
	@Schema(description = "Last TPS value")
	private Float lastTps;

	@PositiveOrZero(message = "Last CPU usage must not be negative")
	@Schema(description = "Last CPU usage value")
	private Float lastUsage;

	@PositiveOrZero(message = "Memory usage must not be negative")
	@Schema(description = "Last memory usage value")
	private Float lastMemoryUsage;

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
		return null;
	}

	@Override
	public void setVersion(Long version) {}

	@Override
	public boolean isNew() {
		return null == this.getId();
	}
}
