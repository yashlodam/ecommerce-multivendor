package com.zosh.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Thrown when an order state transition is not permitted (HTTP 422). */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidOrderStateException extends RuntimeException {
    public InvalidOrderStateException(String message) {
        super(message);
    }
}
