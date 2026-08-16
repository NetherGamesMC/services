package org.nethergames.gsms.domain.model.values;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.nethergames.common.domain.constants.GameType;
import org.nethergames.common.domain.constants.ServerRegion;
import org.nethergames.common.domain.constants.ServerType;
import org.nethergames.common.domain.model.BaseValueObject;

@Schema(description = "Represents the scope for server scaling based on region, server type, and game type.")
public class ScalingScope extends BaseValueObject {

	@Schema(description = "The region in which the server is located.")
	private ServerRegion serverRegion;

	@Schema(description = "The type of server (e.g., BW, FACTIONS, SB).")
	private ServerType serverType;

	@Schema(description = "The type of game (e.g., Solo, Squads, 1v1).")
	private GameType gameType;

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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;

		if (!(o instanceof ScalingScope that)) return false;

		return new EqualsBuilder()
				.append(getServerRegion(), that.getServerRegion())
				.append(getServerType(), that.getServerType())
				.append(getGameType(), that.getGameType())
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).append(getServerRegion()).append(getServerType()).append(getGameType()).toHashCode();
	}
}
