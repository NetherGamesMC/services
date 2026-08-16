package org.nethergames.common.domain.model.values;

import jakarta.validation.constraints.NotNull;
import org.nethergames.common.domain.constants.GameType;
import org.nethergames.common.domain.constants.ServerRegion;
import org.nethergames.common.domain.constants.ServerType;
import org.nethergames.common.domain.model.BaseValueObject;

public class GameServerMetadata extends BaseValueObject {

	@NotNull(message = "Region cannot be null")
	private ServerRegion serverRegion;

	@NotNull(message = "Server type cannot be null")
	private ServerType serverType;

	@NotNull(message = "Game type cannot be null")
	private GameType gameType;

	@NotNull(message = "Deployment ID cannot be null")
	private String deploymentId;

	@NotNull(message = "Replica ID cannot be null")
	private String replicaId;

	public ServerRegion getServerRegion() {
		return serverRegion;
	}

	public void setServerRegion(ServerRegion serverRegion) {
		this.serverRegion = serverRegion;
	}

	public ServerType getServerType() {
		return serverType;
	}

	public void setServerType(ServerType serverType) {
		this.serverType = serverType;
	}

	public GameType getGameType() {
		return gameType;
	}

	public void setGameType(GameType gameType) {
		this.gameType = gameType;
	}

	public String getDeploymentId() {
		return deploymentId;
	}

	public void setDeploymentId(String deploymentId) {
		this.deploymentId = deploymentId;
	}

	public String getReplicaId() {
		return replicaId;
	}

	public void setReplicaId(String replicaId) {
		this.replicaId = replicaId;
	}

	public String toString() {
		if (gameType == GameType.NONE) {
			return serverRegion.name().toUpperCase() + "-" +
					serverType.getServerType() + "--" +
					deploymentId + "-" + replicaId;
		}

		return serverRegion.name().toUpperCase() + "-" +
				serverType.getServerType() + "-" + gameType.getGameType() + "-" +
				deploymentId + "-" + replicaId;
	}
}
