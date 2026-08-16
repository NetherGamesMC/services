package org.nethergames.common.domain.util;

import org.apache.commons.lang3.StringUtils;
import org.nethergames.common.domain.constants.GameType;
import org.nethergames.common.domain.constants.ServerRegion;
import org.nethergames.common.domain.constants.ServerType;
import org.nethergames.common.domain.model.values.GameServerMetadata;

import java.util.InputMismatchException;
import java.util.regex.Pattern;

public final class ServerIdUtil {

	public static final String kubeV1UIDString = "^[a-zA-Z]{2,3}-([a-zA-Z]+)-([a-zA-Z0-9]*)-([a-zA-Z0-9]*)-([a-zA-Z0-9]*)$";
	public static final Pattern kubeV1UIDPattern = Pattern.compile(kubeV1UIDString);

	public static GameServerMetadata fromString(String input) {
		GameServerMetadata uniqueId = new GameServerMetadata();

		if (StringUtils.isNotBlank(input) && kubeV1UIDPattern.matcher(input).matches()) {
			String[] parts = input.split("-");
			uniqueId.setServerRegion(ServerRegion.valueOf(parts[0].toUpperCase()));
			uniqueId.setServerType(ServerType.valueOf(parts[1].toUpperCase()));

			String gameTypeStr = parts[2];
			GameType gameType = GameType.NONE;

			if (StringUtils.isNotBlank(gameTypeStr)) {
				gameType = GameType.of(gameTypeStr.toUpperCase());
			}

			uniqueId.setGameType(gameType);
			uniqueId.setDeploymentId(parts[3]);
			uniqueId.setReplicaId(parts[4]);
			return uniqueId;
		} else {
			throw new InputMismatchException("The given input " + input + " is no valid serverUniqueId");
		}
	}
}
