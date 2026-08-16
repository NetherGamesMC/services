package org.nethergames.gsms.domain.constants;

public enum ServerStatus {
	WAITING(-1),
	RUNNING(0),
	TERMINATING(1);

	private final int statusCode;

	ServerStatus(int statusCode) {
		this.statusCode = statusCode;
	}

	public int getStatusCode() {
		return statusCode;
	}
}
