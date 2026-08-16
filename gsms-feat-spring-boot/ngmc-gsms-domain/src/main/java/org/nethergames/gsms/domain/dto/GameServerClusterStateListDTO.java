package org.nethergames.gsms.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.nethergames.common.domain.constants.GameType;
import org.nethergames.common.domain.constants.ServerType;
import org.nethergames.gsms.domain.dto.values.GameServerClusterDataListDTO;

import java.util.List;

@Schema(description = "DTO representing the state of a game server cluster")
public class GameServerClusterStateListDTO {

	@Schema(description = "Type of the game running on the server cluster")
	private GameType gameType;

	@Schema(description = "Type of the server cluster")
	private ServerType serverType;

	@Schema(description = "Aggregated data of a game server cluster")
	private List<GameServerClusterDataListDTO> clusterData;

	@Schema(description = "Average ticks per second of the server cluster")
	private Float avgTps;

	@Schema(description = "Total number of players in the server cluster")
	private Integer totalPlayers;

	@Schema(description = "Maximum number of players that can be connected to the server cluster")
	private Integer maxPlayers;

	public GameType getGameType() {
		return gameType;
	}

	public void setGameType(GameType gameType) {
		this.gameType = gameType;
	}

	public ServerType getServerType() {
		return serverType;
	}

	public void setServerType(ServerType serverType) {
		this.serverType = serverType;
	}

	public List<GameServerClusterDataListDTO> getClusterData() {
		return clusterData;
	}

	public void setClusterData(List<GameServerClusterDataListDTO> clusterData) {
		this.clusterData = clusterData;
	}

	public Float getAvgTps() {
		return avgTps;
	}

	public void setAvgTps(Float avgTps) {
		this.avgTps = avgTps;
	}

	public Integer getTotalPlayers() {
		return totalPlayers;
	}

	public void setTotalPlayers(Integer totalPlayers) {
		this.totalPlayers = totalPlayers;
	}

	public Integer getMaxPlayers() {
		return maxPlayers;
	}

	public void setMaxPlayers(Integer maxPlayers) {
		this.maxPlayers = maxPlayers;
	}
}
