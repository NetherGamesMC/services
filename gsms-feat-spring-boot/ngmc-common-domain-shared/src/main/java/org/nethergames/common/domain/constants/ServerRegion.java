package org.nethergames.common.domain.constants;

import java.time.ZoneId;

public enum ServerRegion {
	// Timezones based on the region's standard time
	EU("EU", ZoneId.of("UTC+01:00")),
	US("US", ZoneId.of("UTC-05:00")),
	AP("AP", ZoneId.of("UTC+08:00")),
	IND("IND", ZoneId.of("UTC+05:30"));

	private final String region;
	private final ZoneId zoneId;

	ServerRegion(String region, ZoneId zoneId) {
		this.region = region;
		this.zoneId = zoneId;
	}

	public String getRegion() {
		return region;
	}

	public ZoneId getZoneId() {
		return zoneId;
	}
}
