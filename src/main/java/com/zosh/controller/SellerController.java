package com.zosh.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zosh.config.JwtProvider;
import com.zosh.domain.AccountStatus;
import com.zosh.exceptions.DuplicateResourceException;
import com.zosh.exceptions.OtpExpiredException;
import com.zosh.exceptions.ResourceNotFoundException;
import com.zosh.exceptions.SellerException;
import com.zosh.model.Seller;
import com.zosh.model.SellerReport;
import com.zosh.model.VerificationCode;
import com.zosh.repository.VerificationCodeRepository;
import com.zosh.request.SellerRequest;
import com.zosh.response.AuthResponse;
import com.zosh.response.LoginRequest;
import com.zosh.service.AuthService;
import com.zosh.service.EmailService;
import com.zosh.service.SellerReportService;
import com.zosh.service.SellerService;
import com.zosh.utils.OtpUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/sellers")
@Tag(name = "Seller - Onboarding & Account", description = "Seller registration, OTP verification, login, profile, and performance metrics")
public class SellerController {

    @Autowired
    private SellerService sellerService;

    @Autowired
    private AuthService authService;

    @Autowired
    private VerificationCodeRepository verificationCoderepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtProvider jwtprovider;

    @Autowired
    private SellerReportService sellerReportService;

    @Value("${app.otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    // ─── Seller login ─────────────────────────────────────────────────────────

    @PostMapping("/login")
    @Operation(summary = "Authenticate a seller account using email and OTP / password")
    public ResponseEntity<AuthResponse> loginSeller(@Valid @RequestBody LoginRequest req) {
        // Prefix distinguishes seller auth from customer auth in UserDetailsService
        req.setEmail("seller_" + req.getEmail());
        AuthResponse res = authService.siging(req);
        return ResponseEntity.ok(res);
    }

    // ─── Email verification ───────────────────────────────────────────────────

    @PatchMapping("/verify/{otp}")
    @Operation(summary = "Verify seller account email with OTP token")
    public ResponseEntity<Seller> verifySellerEmail(@PathVariable String otp) {
        VerificationCode code = verificationCoderepository.findByOtp(otp);

        if (code == null) {
            throw new ResourceNotFoundException("Verification code for OTP", "otp", otp);
        }
        if (code.isExpired()) {
            verificationCoderepository.delete(code);
            throw new OtpExpiredException(code.getEmail());
        }
        if (!code.getOtp().equals(otp)) {
            throw new IllegalArgumentException("Invalid verification OTP.");
        }

        Seller seller = sellerService.verifyEmail(code.getEmail(), otp);
        verificationCoderepository.delete(code);
        return ResponseEntity.ok(seller);
    }

    // ─── Create seller ────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Register a new seller account (accepts SellerRequest DTO)")
    public ResponseEntity<Seller> createSeller(@Valid @RequestBody SellerRequest req)
            throws DuplicateResourceException {

        Seller savedSeller = sellerService.createSeller(req);

        // Invalidate any stale OTP for this email
        VerificationCode existingCode = verificationCoderepository.findByEmail(req.getEmail());
        if (existingCode != null) {
            verificationCoderepository.delete(existingCode);
        }

        // Send email verification OTP with proper expiration
        String otp = OtpUtil.generateOtp();
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setEmail(req.getEmail());
        verificationCode.setOtp(otp);
        verificationCode.setCreatedAt(java.time.LocalDateTime.now());
        verificationCode.setExpiresAt(java.time.LocalDateTime.now().plusMinutes(otpExpiryMinutes));
        verificationCoderepository.save(verificationCode);

        emailService.sendVerificationOtpEmail(
                req.getEmail(),
                otp,
                "ShopSphere — Verify your seller account",
                "Use the following OTP to verify your seller account."
        );

        return new ResponseEntity<>(savedSeller, HttpStatus.CREATED);
    }

    // ─── Get seller by ID ─────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get public seller profile by ID")
    public ResponseEntity<Seller> getSellerById(@PathVariable Long id) throws SellerException {
        Seller seller = sellerService.getSellerById(id);
        return ResponseEntity.ok(seller);
    }

    // ─── Get seller profile (from JWT) ────────────────────────────────────────

    @GetMapping("/profile")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get authenticated seller's profile details")
    public ResponseEntity<Seller> getSellerByJwt(
            @RequestHeader("Authorization") String jwt) {
        Seller seller = sellerService.getSellerProfile(jwt);
        return ResponseEntity.ok(seller);
    }

    // ─── Seller report ────────────────────────────────────────────────────────

    @GetMapping("/report")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get sales, earnings, and order analytics report for authenticated seller")
    public ResponseEntity<SellerReport> getSellerReport(
            @RequestHeader("Authorization") String jwt) {
        Seller seller = sellerService.getSellerProfile(jwt);
        SellerReport report = sellerReportService.getSellerReport(seller);
        return ResponseEntity.ok(report);
    }

    // ─── List all sellers ─────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List all registered sellers (optional filter by AccountStatus)")
    public ResponseEntity<List<Seller>> getAllSellers(
            @RequestParam(required = false) AccountStatus status) {
        List<Seller> sellers = sellerService.getAllSellers(status);
        return ResponseEntity.ok(sellers);
    }

    // ─── Update seller profile ────────────────────────────────────────────────

    @PatchMapping
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Update authenticated seller's business profile and bank details")
    public ResponseEntity<Seller> updateSeller(
            @RequestHeader("Authorization") String jwt,
            @RequestBody SellerRequest req) throws SellerException {

        Seller profile = sellerService.getSellerProfile(jwt);
        Seller updated = sellerService.updateSeller(profile.getId(), req);
        return ResponseEntity.ok(updated);
    }

    // ─── Delete seller ────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete seller by ID (Admin only)")
    public ResponseEntity<Void> deleteSeller(@PathVariable Long id) throws SellerException {
        sellerService.deleteSeller(id);
        return ResponseEntity.noContent().build();
    }
}
