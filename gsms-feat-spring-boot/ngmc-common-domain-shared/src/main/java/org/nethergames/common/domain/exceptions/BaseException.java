package org.nethergames.common.domain.exceptions;

public abstract class BaseException extends RuntimeException {

	private String code;

	private int httpStatus;

	public BaseException(String code, String message) {
		super(message);
		this.code = code;
	}

	public BaseException(String code, String message, int httpStatus) {
		super(message);
		this.code = code;
		this.httpStatus = httpStatus;
	}

	public BaseException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public BaseException(String code, String message, Throwable throwable) {
		super(message, throwable);
		this.code = code;
	}

	public BaseException(String code, String message, int httpStatus, Throwable throwable) {
		super(message, throwable);
		this.code = code;
		this.httpStatus = httpStatus;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public int getHttpStatus() {
		return httpStatus;
	}

	public void setHttpStatus(int httpStatus) {
		this.httpStatus = httpStatus;
	}
}
