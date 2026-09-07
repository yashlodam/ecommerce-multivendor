package com.zosh.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zosh.domain.USER_ROLE;

public class AuthResponse {

	private String jwt;
	private String message;
	private USER_ROLE role;

	@JsonIgnore
	private String refreshToken;

	public AuthResponse() {
	}

	public AuthResponse(String jwt, String message, USER_ROLE role) {
		this.jwt = jwt;
		this.message = message;
		this.role = role;
	}

	public AuthResponse(String jwt, String message, USER_ROLE role, String refreshToken) {
		this.jwt = jwt;
		this.message = message;
		this.role = role;
		this.refreshToken = refreshToken;
	}

	public String getJwt() {
		return jwt;
	}
	public void setJwt(String jwt) {
		this.jwt = jwt;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public USER_ROLE getRole() {
		return role;
	}
	public void setRole(USER_ROLE role) {
		this.role = role;
	}
	public String getRefreshToken() {
		return refreshToken;
	}
	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
}
