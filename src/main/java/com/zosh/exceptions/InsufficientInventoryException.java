package com.zosh.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when inventory is insufficient to fulfill the requested order quantity (HTTP 409).
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class InsufficientInventoryException extends RuntimeException {

    private final Long productId;
    private final Integer requested;
    private final Integer available;

    public InsufficientInventoryException(Long productId, Integer requested, Integer available) {
        super(String.format(
                "Insufficient inventory for product %d: requested %d, available %d",
                productId, requested, available));
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public Long getProductId()    { return productId; }
    public Integer getRequested() { return requested; }
    public Integer getAvailable() { return available; }
}
