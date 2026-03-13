package com.workflowy.data.converter;

public class WorkflowyApiException extends RuntimeException {

	public WorkflowyApiException(String message) {
		super(message);
	}

	public WorkflowyApiException(String message, Throwable cause) {
		super(message, cause);
	}
}
