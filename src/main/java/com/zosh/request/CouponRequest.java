package com.zosh.request;

import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class CouponRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;

    @DecimalMin(value = "1.0", message = "Discount percentage must be at least 1%")
    @DecimalMax(value = "100.0", message = "Discount percentage cannot exceed 100%")
    private double discountPercentage;

    @NotNull(message = "Validity start date is required")
    private LocalDate validityStartDate;

    @NotNull(message = "Validity end date is required")
    @Future(message = "Validity end date must be in the future")
    private LocalDate validityEndDate;

    @PositiveOrZero(message = "Minimum order value cannot be negative")
    private double minimumOrderValue;

    private boolean active = true;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public double getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(double discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public LocalDate getValidityStartDate() {
        return validityStartDate;
    }

    public void setValidityStartDate(LocalDate validityStartDate) {
        this.validityStartDate = validityStartDate;
    }

    public LocalDate getValidityEndDate() {
        return validityEndDate;
    }

    public void setValidityEndDate(LocalDate validityEndDate) {
        this.validityEndDate = validityEndDate;
    }

    public double getMinimumOrderValue() {
        return minimumOrderValue;
    }

    public void setMinimumOrderValue(double minimumOrderValue) {
        this.minimumOrderValue = minimumOrderValue;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
