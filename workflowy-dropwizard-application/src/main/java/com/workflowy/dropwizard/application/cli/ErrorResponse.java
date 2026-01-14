package com.workflowy.dropwizard.application.cli;

public class ErrorResponse {

	private final boolean success = false;
	private final String errorCode;
	private final String message;

	public ErrorResponse(String errorCode, String message) {
		this.errorCode = errorCode;
		this.message = message;
	}

	public boolean isSuccess() {
		return this.success;
	}

	public String getErrorCode() {
		return this.errorCode;
	}

	public String getMessage() {
		return this.message;
	}
}
