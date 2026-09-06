package com.zosh.exceptions;

import java.time.LocalDateTime;

/**
 * Structured error response returned by GlobalException.
 * Consistent shape for all API errors.
 */
public class ErrorDetails {

    private LocalDateTime timestamp;
    private int status;
    private String code;
    private String message;
    private String path;

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    // Legacy fields preserved for backwards compatibility
    public String getError() { return message; }
    public String getDetails() { return path; }
}
