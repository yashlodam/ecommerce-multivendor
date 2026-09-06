package com.zosh.exceptions;

/**
 * Exception thrown when transactional email dispatch via external API (e.g. Brevo) fails.
 * Ensures caller receives an explicit error rather than a false positive success response.
 */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message) {
        super(message);
    }

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
