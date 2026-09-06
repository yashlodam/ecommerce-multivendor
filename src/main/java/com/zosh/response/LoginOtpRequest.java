package com.zosh.response;

import com.zosh.domain.USER_ROLE;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginOtpRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    private String otp;

    private USER_ROLE role = USER_ROLE.ROLE_CUSTOMER;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public USER_ROLE getRole() {
        return role;
    }

    public void setRole(USER_ROLE role) {
        this.role = role;
    }
}
