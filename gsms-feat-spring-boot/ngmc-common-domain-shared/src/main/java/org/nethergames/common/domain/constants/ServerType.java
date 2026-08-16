package org.nethergames.common.domain.constants;

public enum ServerType {
	AC("ac"),
	BW("bw"),
	CQ("cq"),
	CREATIVE("creative"),
	DUELS("duels"),
	FACTIONS("factions"),
	LOBBY("lobby"),
	MD("md"),
	MM("mm"),
	MS("ms"),
	REPLAY("replay"),
	SB("sb"),
	SC("sc"),
	SETUP("setup"),
	SG("sg"),
	SW("sw"),
	TB("tb"),
	UHC("uhc");

	private final String serverType;

	ServerType(String serverType) {
		this.serverType = serverType;
	}

	public String getServerType() {
		return serverType;
	}
}
