package com.zosh.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.zosh.config.JwtProvider;
import com.zosh.domain.USER_ROLE;
import com.zosh.exceptions.DuplicateResourceException;
import com.zosh.exceptions.OtpExpiredException;
import com.zosh.model.Cart;
import com.zosh.model.User;
import com.zosh.model.VerificationCode;
import com.zosh.repository.CartRepository;
import com.zosh.repository.SellerRepository;
import com.zosh.repository.UserRepository;
import com.zosh.repository.VerificationCodeRepository;
import com.zosh.response.SignupRequest;
import com.zosh.service.EmailService;

/**
 * Unit tests for AuthServiceImpl — critical auth flow.
 *
 * Tests verify:
 * 1. Password is encoded exactly ONCE (critical bug fix)
 * 2. OTP expiry is enforced
 * 3. Duplicate email registration is rejected
 * 4. Invalid OTP is rejected
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepo;
    @Mock private CartRepository cartRepo;
    @Mock private VerificationCodeRepository verificationCodeRepo;
    @Mock private EmailService emailService;
    @Mock private SellerRepository sellerRepo;
    @Mock private CustomeUserServiceImpl customUserService;
    @Mock private JwtProvider jwtProvider;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "otpExpiryMinutes", 10);
    }

    private VerificationCode validOtp(String email, String otp) {
        VerificationCode vc = new VerificationCode();
        vc.setEmail(email);
        vc.setOtp(otp);
        vc.setCreatedAt(LocalDateTime.now());
        vc.setExpiresAt(LocalDateTime.now().plusMinutes(10)); // not expired
        return vc;
    }

    private VerificationCode expiredOtp(String email, String otp) {
        VerificationCode vc = new VerificationCode();
        vc.setEmail(email);
        vc.setOtp(otp);
        vc.setCreatedAt(LocalDateTime.now().minusMinutes(20));
        vc.setExpiresAt(LocalDateTime.now().minusMinutes(10)); // expired!
        return vc;
    }

    @Test
    @DisplayName("createUser: password should be encoded EXACTLY once (critical bug fix)")
    void createUser_passwordEncodedOnce() {
        String email = "test@example.com";
        String otp   = "123456";

        SignupRequest req = new SignupRequest();
        req.setEmail(email);
        req.setFullName("Test User");
        req.setOtp(otp);

        when(verificationCodeRepo.findByEmail(email)).thenReturn(validOtp(email, otp));
        when(userRepo.existsByEmail(email)).thenReturn(false);
        when(userRepo.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(cartRepo.save(any())).thenReturn(new Cart());
        when(jwtProvider.generateToken(any())).thenReturn("jwt-token");

        authService.createUser(req);

        // Capture the saved user and verify password encoding
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        String savedPassword = savedUser.getPassword();

        // Password must be a valid BCrypt hash of the OTP
        assertTrue(passwordEncoder.matches(otp, savedPassword),
                "Password should match single-encoded OTP");

        // CRITICAL: password must NOT be double-encoded
        // If double-encoded, matches() would return false
        assertFalse(passwordEncoder.matches(passwordEncoder.encode(otp), savedPassword),
                "Password must NOT be double-encoded");
    }

    @Test
    @DisplayName("createUser: should throw OtpExpiredException when OTP is expired")
    void createUser_expiredOtp_throwsException() {
        String email = "test@example.com";
        String otp   = "123456";

        SignupRequest req = new SignupRequest();
        req.setEmail(email);
        req.setFullName("Test User");
        req.setOtp(otp);

        when(verificationCodeRepo.findByEmail(email)).thenReturn(expiredOtp(email, otp));

        assertThrows(OtpExpiredException.class, () -> authService.createUser(req));
    }

    @Test
    @DisplayName("createUser: should throw BadCredentialsException when OTP is wrong")
    void createUser_wrongOtp_throwsException() {
        String email = "test@example.com";

        SignupRequest req = new SignupRequest();
        req.setEmail(email);
        req.setFullName("Test User");
        req.setOtp("wrong-otp");

        when(verificationCodeRepo.findByEmail(email)).thenReturn(validOtp(email, "correct-otp"));

        assertThrows(BadCredentialsException.class, () -> authService.createUser(req));
    }

    @Test
    @DisplayName("createUser: should throw DuplicateResourceException when email already registered")
    void createUser_duplicateEmail_throwsException() {
        String email = "existing@example.com";
        String otp   = "123456";

        SignupRequest req = new SignupRequest();
        req.setEmail(email);
        req.setFullName("Test User");
        req.setOtp(otp);

        when(verificationCodeRepo.findByEmail(email)).thenReturn(validOtp(email, otp));
        when(userRepo.existsByEmail(email)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.createUser(req));
    }

    @Test
    @DisplayName("createUser: user mobile should NOT be hardcoded to developer number")
    void createUser_mobileNotHardcoded() {
        String email = "test@example.com";
        String otp   = "123456";

        SignupRequest req = new SignupRequest();
        req.setEmail(email);
        req.setFullName("Test User");
        req.setOtp(otp);

        when(verificationCodeRepo.findByEmail(email)).thenReturn(validOtp(email, otp));
        when(userRepo.existsByEmail(email)).thenReturn(false);
        when(userRepo.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(cartRepo.save(any())).thenReturn(new Cart());
        when(jwtProvider.generateToken(any())).thenReturn("jwt-token");

        authService.createUser(req);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        // Mobile should NOT be hardcoded to the developer's personal number
        assertNotEquals("9665774924", savedUser.getMobile(),
                "Mobile number must NOT be hardcoded to developer's personal phone number");
    }

    @Test
    @DisplayName("createUser: OTP should be deleted after successful registration (single use)")
    void createUser_otpDeletedAfterUse() {
        String email = "test@example.com";
        String otp   = "123456";
        VerificationCode vc = validOtp(email, otp);

        SignupRequest req = new SignupRequest();
        req.setEmail(email);
        req.setFullName("Test User");
        req.setOtp(otp);

        when(verificationCodeRepo.findByEmail(email)).thenReturn(vc);
        when(userRepo.existsByEmail(email)).thenReturn(false);
        when(userRepo.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(cartRepo.save(any())).thenReturn(new Cart());
        when(jwtProvider.generateToken(any())).thenReturn("jwt-token");

        authService.createUser(req);

        // OTP should be consumed after use to prevent replay attacks
        verify(verificationCodeRepo).delete(vc);
    }

    @Test
    @DisplayName("sentLoginOtp: saves OTP, invokes email service, and returns generated OTP")
    void sentLoginOtp_savesOtpAndReturnsValue() {
        String email = "customer@example.com";
        when(verificationCodeRepo.findByEmail(email)).thenReturn(null);

        String returnedOtp = authService.sentLoginOtp(email, USER_ROLE.ROLE_CUSTOMER);

        assertNotNull(returnedOtp);
        assertEquals(6, returnedOtp.length());
        verify(verificationCodeRepo).save(any(VerificationCode.class));
        verify(emailService).sendVerificationOtpEmail(eq(email), eq(returnedOtp), anyString(), anyString());
    }

    @Test
    @DisplayName("sentLoginOtp: invalidates previous OTP before issuing new one")
    void sentLoginOtp_replacesPreviousOtp() {
        String email = "customer@example.com";
        VerificationCode oldVc = validOtp(email, "999999");
        when(verificationCodeRepo.findByEmail(email)).thenReturn(oldVc);

        String returnedOtp = authService.sentLoginOtp(email, USER_ROLE.ROLE_CUSTOMER);

        assertNotNull(returnedOtp);
        verify(verificationCodeRepo).delete(oldVc);
        verify(verificationCodeRepo).save(any(VerificationCode.class));
    }
}
