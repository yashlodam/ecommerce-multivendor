package com.zosh.response;

public class CurrentUserResponse {
   
	private String role;

	
	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public CurrentUserResponse(String role) {
		super();
		this.role = role;
	}
	
	
}
