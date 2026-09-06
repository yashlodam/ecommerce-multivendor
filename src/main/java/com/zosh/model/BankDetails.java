package com.zosh.model;

import jakarta.persistence.Embeddable;

/**
 * Embedded bank details for a Seller.
 * Stored as columns in the seller table (not a separate entity).
 *
 * NOTE: Account number is sensitive — do not expose in API responses carelessly.
 */
@Embeddable
public class BankDetails {

    private String accountNumber;

    private String accountHolderName;

    private String ifscCode;

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }
}
