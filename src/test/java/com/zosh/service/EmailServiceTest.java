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

import com.fasterxml.jackson.databind.ObjectMapper;

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
    @DisplayName("sendVerificationOtpEmail: when RESEND_API_KEY is not configured, skip dispatch")
    void sendVerificationOtpEmail_missingApiKey_skips() throws Exception {
        emailService.setResendApiKey("");

        emailService.sendVerificationOtpEmail("user@example.com", "123456", "Test Subject", "Test text");

        verifyNoInteractions(mockHttpClient);
    }

    @Test
    @DisplayName("sendVerificationOtpEmail: successful dispatch sends HTTPS POST to Resend")
    void sendVerificationOtpEmail_success() throws Exception {
        emailService.setResendApiKey("re_test_key_123");
        emailService.setResendFrom("ShopSphere <onboarding@resend.dev>");

        when(mockHttpResponse.statusCode()).thenReturn(200);
        doReturn(mockHttpResponse).when(mockHttpClient).send(any(HttpRequest.class), any());

        emailService.sendVerificationOtpEmail("customer@example.com", "654321", "Verify Account", "Your code is");

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient, times(1)).send(requestCaptor.capture(), any());

        HttpRequest captured = requestCaptor.getValue();
        assertEquals("https://api.resend.com/emails", captured.uri().toString());
        assertEquals("POST", captured.method());
        assertTrue(captured.headers().firstValue("Authorization").orElse("").contains("re_test_key_123"));
        assertTrue(captured.headers().firstValue("Content-Type").orElse("").contains("application/json"));
    }

    @Test
    @DisplayName("sendVerificationOtpEmail: handles Resend non-200 HTTP response gracefully without throwing")
    void sendVerificationOtpEmail_non200_doesNotThrow() throws Exception {
        emailService.setResendApiKey("re_test_key_123");

        when(mockHttpResponse.statusCode()).thenReturn(422);
        doReturn(mockHttpResponse).when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() -> {
            emailService.sendVerificationOtpEmail("customer@example.com", "112233", "Verify", "Code");
        });

        verify(mockHttpClient, times(1)).send(any(), any());
    }

    @Test
    @DisplayName("sendVerificationOtpEmail: handles network exception gracefully without throwing")
    void sendVerificationOtpEmail_networkException_doesNotThrow() throws Exception {
        emailService.setResendApiKey("re_test_key_123");

        when(mockHttpClient.send(any(HttpRequest.class), any())).thenThrow(new IOException("Connection reset"));

        assertDoesNotThrow(() -> {
            emailService.sendVerificationOtpEmail("customer@example.com", "998877", "Verify", "Code");
        });
    }
}
