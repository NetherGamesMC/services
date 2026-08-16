package org.nethergames.gsms.domain.model;

import org.nethergames.common.domain.model.BaseEntity;
import org.nethergames.common.domain.model.values.GameServerMetadata;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.TimeSeries;
import org.springframework.data.mongodb.core.timeseries.Granularity;

import java.time.Instant;

@TimeSeries(collection = GameServerMetrics.COLL_NAME,
		timeField = "ts", metaField = "meta", granularity = Granularity.MINUTES,
		expireAfter = "14d")
public class GameServerMetrics extends BaseEntity<String> {

	public static final @Transient String COLL_NAME = "gs_game_server_metrics";

	@Field("ts")
	private Instant timestamp;

	@Field("meta")
	private GameServerMetadata metadata;

	@Field("pc")
	private Integer playerCount;

	@Field("lt")
	private Float lastTps;

	@Field("lu")
	private Float lastUsage;

	@Field("lm")
	private Float lastMemoryUsage;

	@Field("ag")
	private Integer activeMatches;

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
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

	public Integer getActiveMatches() {
		return activeMatches;
	}

	public void setActiveMatches(Integer activeMatches) {
		this.activeMatches = activeMatches;
	}
}
