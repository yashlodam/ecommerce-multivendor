package com.zosh.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Thrown when an OTP has expired (HTTP 400). */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class OtpExpiredException extends RuntimeException {
    public OtpExpiredException(String email) {
        super("The OTP for '" + email + "' has expired. Please request a new OTP.");
    }
}
