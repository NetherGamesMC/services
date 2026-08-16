package org.nethergames.gsms.domain.dto.values;

import io.swagger.v3.oas.annotations.media.Schema;
import org.nethergames.common.domain.model.BaseValueObject;

@Schema(description = "DTO representing the aggregated data of a game server cluster")
public class GameServerClusterDataListDTO extends BaseValueObject {

	@Schema(description = "Kubernetes server unique identifier")
	private String serverId;

	@Schema(description = "Maximum number of players that can be connected to the server")
	private int maxPlayers;

	@Schema(description = "Total number of players in the server")
	private int totalPlayers;

	@Schema(description = "Average ticks per second of the server")
	private Float tps;

	@Schema(description = "Average CPU usage of the server")
	private Float usage;

	public String getServerId() {
		return serverId;
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	public int getMaxPlayers() {
		return maxPlayers;
	}

	public void setMaxPlayers(int maxPlayers) {
		this.maxPlayers = maxPlayers;
	}

	public int getTotalPlayers() {
		return totalPlayers;
	}

	public void setTotalPlayers(int totalPlayers) {
		this.totalPlayers = totalPlayers;
	}

	public Float getTps() {
		return tps;
	}

	public void setTps(Float tps) {
		this.tps = tps;
	}

	public Float getUsage() {
		return usage;
	}

	public void setUsage(Float usage) {
		this.usage = usage;
	}
}
