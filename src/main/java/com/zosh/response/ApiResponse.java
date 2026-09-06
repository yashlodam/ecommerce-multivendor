package com.zosh.response;

import java.time.LocalDateTime;

public class ApiResponse {

    private boolean success = true;
    private String message;
    private String otp;
    private LocalDateTime timestamp = LocalDateTime.now();

    public ApiResponse() {
    }

    public ApiResponse(String message) {
        this.message = message;
        this.success = true;
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(String message, String otp) {
        this.message = message;
        this.otp = otp;
        this.success = true;
        this.timestamp = LocalDateTime.now();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
