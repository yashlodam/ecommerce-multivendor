package com.zosh.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for seller registration.
 *
 * NEVER accept the Seller JPA entity directly from the client for registration.
 * This DTO:
 *  - Prevents the client from injecting an existing Address ID (detached-entity bug)
 *  - Prevents the client from controlling id, role, accountStatus, isEmailVerified
 *  - Validates input via Bean Validation before it reaches the service
 */
public class SellerRequest {

    @NotBlank(message = "Seller name is required")
    private String sellerName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String mobile;

    @JsonProperty("gstin")
    @JsonAlias({"GSTIN", "gstin", "gstinNumber", "gst_number", "gstNumber", "gSTIN"})
    private String GSTIN;

    // Embedded — sent inline, not as a separate entity reference
    private BusinessDetailsRequest businessDetails;

    private BankDetailsRequest bankDetails;

    @Valid
    private AddressRequest pickupAddress;

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    @JsonProperty("gstin")
    public String getGSTIN() { return GSTIN; }

    @JsonProperty("gstin")
    @JsonAlias({"GSTIN", "gstin", "gstinNumber", "gst_number", "gstNumber", "gSTIN"})
    public void setGSTIN(String GSTIN) { this.GSTIN = GSTIN; }

    public BusinessDetailsRequest getBusinessDetails() { return businessDetails; }
    public void setBusinessDetails(BusinessDetailsRequest businessDetails) { this.businessDetails = businessDetails; }

    public BankDetailsRequest getBankDetails() { return bankDetails; }
    public void setBankDetails(BankDetailsRequest bankDetails) { this.bankDetails = bankDetails; }

    public AddressRequest getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(AddressRequest pickupAddress) { this.pickupAddress = pickupAddress; }

    // ─── Nested DTOs (static inner classes for cohesion) ────────────────────

    public static class AddressRequest {
        private String name;
        private String locality;
        private String address;
        @NotBlank(message = "City is required")
        private String city;
        @NotBlank(message = "State is required")
        private String state;
        private String pinCode;
        private String mobile;

        // No 'id' field — prevents client from attaching an existing Address

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getLocality() { return locality; }
        public void setLocality(String locality) { this.locality = locality; }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }

        public String getPinCode() { return pinCode; }
        public void setPinCode(String pinCode) { this.pinCode = pinCode; }

        public String getMobile() { return mobile; }
        public void setMobile(String mobile) { this.mobile = mobile; }
    }

    public static class BusinessDetailsRequest {
        private String businessName;
        private String businessEmail;
        private String businessMobile;
        private String businessAddress;
        private String logo;
        private String banner;

        public String getBusinessName() { return businessName; }
        public void setBusinessName(String businessName) { this.businessName = businessName; }

        public String getBusinessEmail() { return businessEmail; }
        public void setBusinessEmail(String businessEmail) { this.businessEmail = businessEmail; }

        public String getBusinessMobile() { return businessMobile; }
        public void setBusinessMobile(String businessMobile) { this.businessMobile = businessMobile; }

        public String getBusinessAddress() { return businessAddress; }
        public void setBusinessAddress(String businessAddress) { this.businessAddress = businessAddress; }

        public String getLogo() { return logo; }
        public void setLogo(String logo) { this.logo = logo; }

        public String getBanner() { return banner; }
        public void setBanner(String banner) { this.banner = banner; }
    }

    public static class BankDetailsRequest {
        private String accountNumber;
        private String accountHolderName;
        private String ifscCode;

        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

        public String getAccountHolderName() { return accountHolderName; }
        public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }

        public String getIfscCode() { return ifscCode; }
        public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
    }
}
