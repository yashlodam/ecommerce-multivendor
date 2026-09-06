package com.zosh.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserUpdateRequest {

    private String fullName;
    private String mobile;
    private String email;
    private UserUpdateRequest userData;

    public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public UserUpdateRequest() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public UserUpdateRequest getUserData() {
        return userData;
    }

    public void setUserData(UserUpdateRequest userData) {
        this.userData = userData;
    }

    public String getEffectiveFullName() {
        if (fullName != null && !fullName.isBlank()) {
            return fullName.trim();
        }
        if (userData != null && userData.getFullName() != null && !userData.getFullName().isBlank()) {
            return userData.getFullName().trim();
        }
        return null;
    }

    public String getEffectiveMobile() {
        if (mobile != null && !mobile.isBlank()) {
            return mobile.trim();
        }
        if (userData != null && userData.getMobile() != null && !userData.getMobile().isBlank()) {
            return userData.getMobile().trim();
        }
        return null;
    }
}