package com.zosh.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zosh.exceptions.EmailDeliveryException;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    private EmailService emailService;

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockHttpResponse;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        emailService = new EmailService();
        ReflectionTestUtils.setField(emailService, "objectMapper", objectMapper);
        emailService.setHttpClient(mockHttpClient);
    }

    @Test
    @DisplayName("sendVerificationOtpEmail: when BREVO_API_KEY is not configured, throw EmailDeliveryException")
    void sendVerificationOtpEmail_missingApiKey_throwsException() {
        emailService.setBrevoApiKey("");

        EmailDeliveryException ex = assertThrows(EmailDeliveryException.class, () -> {
            emailService.sendVerificationOtpEmail("user@example.com", "123456", "Test Subject", "Test text");
        });

        assertTrue(ex.getMessage().contains("BREVO_API_KEY"));
        verifyNoInteractions(mockHttpClient);
    }

    @Test
    @DisplayName("sendVerificationOtpEmail: successful dispatch sends HTTPS POST to Brevo API with valid payload")
    void sendVerificationOtpEmail_success() throws Exception {
        emailService.setBrevoApiKey("xkeysib-test-key-12345");
        emailService.setBrevoFromEmail("noreply@shopsphere.com");
        emailService.setBrevoFromName("ShopSphere");

        when(mockHttpResponse.statusCode()).thenReturn(201);
        doReturn(mockHttpResponse).when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() -> {
            emailService.sendVerificationOtpEmail("customer@example.com", "654321", "Verify Account", "Your verification code is:");
        });

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient, times(1)).send(requestCaptor.capture(), any());

        HttpRequest captured = requestCaptor.getValue();
        assertEquals("https://api.brevo.com/v3/smtp/email", captured.uri().toString());
        assertEquals("POST", captured.method());
        assertEquals("xkeysib-test-key-12345", captured.headers().firstValue("api-key").orElse(""));
        assertEquals("application/json", captured.headers().firstValue("Content-Type").orElse(""));
        assertEquals("application/json", captured.headers().firstValue("Accept").orElse(""));
    }

    @Test
    @DisplayName("sendVerificationOtpEmail: Brevo 400 Bad Request throws EmailDeliveryException (e.g. unverified sender)")
    void sendVerificationOtpEmail_status400_throwsEmailDeliveryException() throws Exception {
        emailService.setBrevoApiKey("xkeysib-test-key-12345");
        emailService.setBrevoFromEmail("unverified@shopsphere.com");

        when(mockHttpResponse.statusCode()).thenReturn(400);
        when(mockHttpResponse.body()).thenReturn("{\"code\":\"invalid_parameter\",\"message\":\"sender email is not valid or verified\"}");
        doReturn(mockHttpResponse).when(mockHttpClient).send(any(HttpRequest.class), any());

        EmailDeliveryException ex = assertThrows(EmailDeliveryException.class, () -> {
            emailService.sendVerificationOtpEmail("customer@example.com", "112233", "Verify", "Code");
        });

        assertTrue(ex.getMessage().contains("verified sender"));
        verify(mockHttpClient, times(1)).send(any(), any());
    }

    @Test
    @DisplayName("sendVerificationOtpEmail: Brevo 401 Unauthorized throws EmailDeliveryException")
    void sendVerificationOtpEmail_status401_throwsEmailDeliveryException() throws Exception {
        emailService.setBrevoApiKey("bad-api-key");

        when(mockHttpResponse.statusCode()).thenReturn(401);
        when(mockHttpResponse.body()).thenReturn("{\"code\":\"unauthorized\",\"message\":\"Key not found\"}");
        doReturn(mockHttpResponse).when(mockHttpClient).send(any(HttpRequest.class), any());

        EmailDeliveryException ex = assertThrows(EmailDeliveryException.class, () -> {
            emailService.sendVerificationOtpEmail("customer@example.com", "112233", "Verify", "Code");
        });

        assertTrue(ex.getMessage().contains("authentication failed"));
    }

    @Test
    @DisplayName("sendVerificationOtpEmail: Brevo 429 Rate Limit throws EmailDeliveryException")
    void sendVerificationOtpEmail_status429_throwsEmailDeliveryException() throws Exception {
        emailService.setBrevoApiKey("xkeysib-test-key-12345");

        when(mockHttpResponse.statusCode()).thenReturn(429);
        when(mockHttpResponse.body()).thenReturn("{\"code\":\"too_many_requests\",\"message\":\"Rate limit exceeded\"}");
        doReturn(mockHttpResponse).when(mockHttpClient).send(any(HttpRequest.class), any());

        EmailDeliveryException ex = assertThrows(EmailDeliveryException.class, () -> {
            emailService.sendVerificationOtpEmail("customer@example.com", "112233", "Verify", "Code");
        });

        assertTrue(ex.getMessage().toLowerCase().contains("rate limit"));
    }

    @Test
    @DisplayName("sendVerificationOtpEmail: Brevo 500 server error throws EmailDeliveryException")
    void sendVerificationOtpEmail_status500_throwsEmailDeliveryException() throws Exception {
        emailService.setBrevoApiKey("xkeysib-test-key-12345");

        when(mockHttpResponse.statusCode()).thenReturn(500);
        when(mockHttpResponse.body()).thenReturn("{\"code\":\"server_error\",\"message\":\"Internal server error\"}");
        doReturn(mockHttpResponse).when(mockHttpClient).send(any(HttpRequest.class), any());

        EmailDeliveryException ex = assertThrows(EmailDeliveryException.class, () -> {
            emailService.sendVerificationOtpEmail("customer@example.com", "112233", "Verify", "Code");
        });

        assertTrue(ex.getMessage().contains("500"));
    }

    @Test
    @DisplayName("sendVerificationOtpEmail: network IOException throws EmailDeliveryException")
    void sendVerificationOtpEmail_networkException_throwsEmailDeliveryException() throws Exception {
        emailService.setBrevoApiKey("xkeysib-test-key-12345");

        when(mockHttpClient.send(any(HttpRequest.class), any())).thenThrow(new IOException("Connection reset"));

        EmailDeliveryException ex = assertThrows(EmailDeliveryException.class, () -> {
            emailService.sendVerificationOtpEmail("customer@example.com", "998877", "Verify", "Code");
        });

        assertTrue(ex.getMessage().contains("Network connection failed"));
    }
}
