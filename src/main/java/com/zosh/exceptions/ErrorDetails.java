package com.zosh.exceptions;

import java.time.LocalDateTime;

public class ErrorDetails {

	private String error;
	private String details;
	private LocalDateTime timestamp;
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}
	public String getDetails() {
		return details;
	}
	public void setDetails(String details) {
		this.details = details;
	}
	public LocalDateTime getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}
	
}
