package com.zosh.exceptions;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.razorpay.RazorpayException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Centralised exception handler — all unhandled exceptions pass through here.
 *
 * Returns a consistent JSON error structure:
 * {
 *   "timestamp": "2024-...",
 *   "status": 400,
 *   "code": "VALIDATION_ERROR",
 *   "message": "...",
 *   "path": "..."
 * }
 *
 * IMPORTANT: Stack traces are NEVER exposed to the client.
 * They are logged internally with the full details.
 */
@RestControllerAdvice
public class GlobalException {

    private static final Logger log = LoggerFactory.getLogger(GlobalException.class);

    // ---- Domain Exceptions ----

    @ExceptionHandler(SellerException.class)
    public ResponseEntity<ErrorDetails> handleSellerException(SellerException ex, WebRequest req) {
        log.warn("SellerException: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "SELLER_ERROR", ex.getMessage(), req);
    }

    @ExceptionHandler(ProductException.class)
    public ResponseEntity<ErrorDetails> handleProductException(ProductException ex, WebRequest req) {
        log.warn("ProductException: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "PRODUCT_ERROR", ex.getMessage(), req);
    }

    @ExceptionHandler(RazorpayException.class)
    public ResponseEntity<ErrorDetails> handleRazorpayException(RazorpayException ex, WebRequest req) {
        log.error("Razorpay payment gateway exception: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "PAYMENT_GATEWAY_ERROR",
                "Payment gateway error: " + ex.getMessage(), req);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleNotFound(ResourceNotFoundException ex, WebRequest req) {
        log.warn("ResourceNotFound: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorDetails> handleDuplicate(DuplicateResourceException ex, WebRequest req) {
        log.warn("DuplicateResource: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", ex.getMessage(), req);
    }

    @ExceptionHandler(InsufficientInventoryException.class)
    public ResponseEntity<ErrorDetails> handleInventory(InsufficientInventoryException ex, WebRequest req) {
        log.warn("InsufficientInventory: product={} requested={} available={}",
                ex.getProductId(), ex.getRequested(), ex.getAvailable());
        return buildError(HttpStatus.CONFLICT, "INSUFFICIENT_INVENTORY", ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public ResponseEntity<ErrorDetails> handleOrderState(InvalidOrderStateException ex, WebRequest req) {
        log.warn("InvalidOrderState: {}", ex.getMessage());
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_ORDER_STATE", ex.getMessage(), req);
    }

    @ExceptionHandler(OtpExpiredException.class)
    public ResponseEntity<ErrorDetails> handleOtpExpired(OtpExpiredException ex, WebRequest req) {
        log.warn("OtpExpired: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "OTP_EXPIRED", ex.getMessage(), req);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorDetails> handleIllegalState(IllegalStateException ex, WebRequest req) {
        log.warn("IllegalStateException: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "ILLEGAL_STATE", ex.getMessage(), req);
    }

    // ---- Validation Exceptions ----

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDetails> handleValidation(MethodArgumentNotValidException ex, WebRequest req) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        String message = "Validation failed: " + fieldErrors;
        log.warn("ValidationError: {}", fieldErrors);
        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, req);
    }

    // ---- Security Exceptions ----

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDetails> handleAccessDenied(AccessDeniedException ex, WebRequest req) {
        log.warn("AccessDenied: {} - {}", req.getDescription(false), ex.getMessage());
        return buildError(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "You do not have permission to perform this action.", req);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorDetails> handleAuthentication(AuthenticationException ex, WebRequest req) {
        log.warn("AuthenticationFailure: {}", ex.getMessage());
        return buildError(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required.", req);
    }

    // ---- Illegal Argument ----

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDetails> handleIllegalArgument(IllegalArgumentException ex, WebRequest req) {
        log.warn("IllegalArgument: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), req);
    }

    // ---- HTTP Request & JSON Parsing Exceptions ----

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDetails> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, WebRequest req) {
        log.warn("Malformed JSON: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "MALFORMED_JSON",
                "Malformed JSON request body or incompatible field value.", req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDetails> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest req) {
        String param = ex.getName();
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String message = String.format("Parameter '%s' should be of type '%s'.", param, expectedType);
        log.warn("TypeMismatch: {}", message);
        return buildError(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT_TYPE", message, req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorDetails> handleMissingParam(MissingServletRequestParameterException ex, WebRequest req) {
        String message = String.format("Required query parameter '%s' is missing.", ex.getParameterName());
        log.warn("MissingParam: {}", message);
        return buildError(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", message, req);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorDetails> handleMissingHeader(MissingRequestHeaderException ex, WebRequest req) {
        String message = String.format("Required request header '%s' is missing.", ex.getHeaderName());
        log.warn("MissingHeader: {}", message);
        return buildError(HttpStatus.BAD_REQUEST, "MISSING_HEADER", message, req);
    }

    // ---- Database & Concurrency Exceptions ----

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDetails> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest req) {
        log.warn("DataIntegrityViolation: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "Database constraint violation. The requested operation conflicts with existing data.", req);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorDetails> handleOptimisticLock(ObjectOptimisticLockingFailureException ex, WebRequest req) {
        log.warn("OptimisticLockFailure: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "The resource was modified by another operation. Please refresh and try again.", req);
    }

    // ---- Catch-All (prevents stack trace exposure) ----

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleAll(Exception ex, WebRequest req) {
        // Log the full stack trace internally, but never expose it to the client
        log.error("Unexpected error at {}: {}", req.getDescription(false), ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later.", req);
    }

    // ---- Helper ----

    private ResponseEntity<ErrorDetails> buildError(
            HttpStatus status, String code, String message, WebRequest req) {

        ErrorDetails error = new ErrorDetails();
        error.setTimestamp(LocalDateTime.now());
        error.setStatus(status.value());
        error.setCode(code);
        error.setMessage(message);
        error.setPath(req.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(error, status);
    }
}