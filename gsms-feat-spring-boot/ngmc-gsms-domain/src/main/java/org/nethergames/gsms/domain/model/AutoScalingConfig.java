package org.nethergames.gsms.domain.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.nethergames.common.domain.model.BaseEntity;
import org.nethergames.common.domain.model.values.GameServerMetadata;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalTime;

/*
 * Model entity to represent the game server state automata. This configuration by default will only have a region
 * type saved, if an administrator wants to configure a specific game server, they can do so by specifying more
 * data in the game server metadata. By default, all regions share the same configuration.
 */
@Document(collection = AutoScalingConfig.COLL_NAME)
public class AutoScalingConfig extends BaseEntity<String> {

	public static final @Transient String COLL_NAME = "gs_auto_scaling_config";

	@NotNull
	private GameServerMetadata metadata;

	// Used as a configuration for autoscaling configuration.
	private LocalTime gameStartTime;
	private LocalTime gameEndTime;

	@Min(0)
	private Integer minPlayers;

	@Min(1)
	private Integer maxPlayers;

	@Min(0)
	private Integer activeMatches;

	@Min(0)
	private Integer playersPerMatch;

	public GameServerMetadata getMetadata() {
		return metadata;
	}

	public void setMetadata(GameServerMetadata metadata) {
		this.metadata = metadata;
	}

	public LocalTime getGameStartTime() {
		return gameStartTime;
	}

	public void setGameStartTime(LocalTime gameStartTime) {
		this.gameStartTime = gameStartTime;
	}

	public LocalTime getGameEndTime() {
		return gameEndTime;
	}

	public void setGameEndTime(LocalTime gameEndTime) {
		this.gameEndTime = gameEndTime;
	}

	public Integer getMinPlayers() {
		return minPlayers;
	}

	public void setMinPlayers(Integer minPlayers) {
		this.minPlayers = minPlayers;
	}

	public Integer getMaxPlayers() {
		return maxPlayers;
	}

	public void setMaxPlayers(Integer maxPlayers) {
		this.maxPlayers = maxPlayers;
	}

	public Integer getActiveMatches() {
		return activeMatches;
	}

	public void setActiveMatches(Integer activeMatches) {
		this.activeMatches = activeMatches;
	}

	public Integer getPlayersPerMatch() {
		return playersPerMatch;
	}

	public void setPlayersPerMatch(Integer playersPerMatch) {
		this.playersPerMatch = playersPerMatch;
	}
}
