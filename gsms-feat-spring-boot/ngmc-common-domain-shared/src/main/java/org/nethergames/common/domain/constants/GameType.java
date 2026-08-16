package org.nethergames.common.domain.constants;

public enum GameType {

	NONE("na"),
	DOUBLES("Doubles"),
	TRIOS("Trios"),
	DUELS_DOUBLES("2v2"),
	DUELS_SOLO("1v1"),
	SOLO("Solo"),
	SQUADS("Squads"),
	MEGA("Mega"),
	CLASSIC("Classic"),
	INFECTION("Infection"),
	AGORA("Agora"),
	SKYLAND("Skyland"),
	FARLANDS("Farlands"),
	BADLANDS("Badlands");

	private final String gameType;

	GameType(String gameType) {
		this.gameType = gameType;
	}

	public String getGameType() {
		return gameType;
	}

	public static GameType of(String upperCase) {
		for (GameType gameType : GameType.values()) {
			if (gameType.gameType.equalsIgnoreCase(upperCase)) {
				return gameType;
			}
		}

		return null;
	}
}
