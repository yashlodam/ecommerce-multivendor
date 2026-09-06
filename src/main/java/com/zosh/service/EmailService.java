package com.zosh.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.email.resend.api-key:}")
    private String resendApiKey;

    @Value("${app.email.resend.from:ShopSphere <onboarding@resend.dev>}")
    private String resendFrom;

    private HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    void setResendApiKey(String resendApiKey) {
        this.resendApiKey = resendApiKey;
    }

    void setResendFrom(String resendFrom) {
        this.resendFrom = resendFrom;
    }

    /**
     * Sends a verification OTP email asynchronously via the Resend HTTPS API.
     * Operates over standard HTTPS (port 443), avoiding SMTP port blocks on cloud hosts like Render.
     * Note: In accordance with security standards, OTP values and API keys are never logged.
     */
    @Async
    public void sendVerificationOtpEmail(
            String userEmail,
            String otp,
            String subject,
            String text) {

        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("[Resend Email Service] RESEND_API_KEY is not configured. Email dispatch to {} skipped. Set RESEND_API_KEY in environment to enable email delivery.", userEmail);
            return;
        }

        try {
            String htmlContent = buildHtmlContent(otp, text);

            ObjectNode body = objectMapper.createObjectNode();
            body.put("from", resendFrom != null && !resendFrom.isBlank() ? resendFrom.trim() : "ShopSphere <onboarding@resend.dev>");
            ArrayNode toArray = body.putArray("to");
            toArray.add(userEmail);
            body.put("subject", subject);
            body.put("html", htmlContent);

            String requestBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RESEND_API_URL))
                    .header("Authorization", "Bearer " + resendApiKey.trim())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("OTP verification email dispatched successfully via Resend HTTPS API to {}", userEmail);
            } else {
                log.error("Resend API returned non-success response for {}: HTTP status {}", userEmail, response.statusCode());
            }

        } catch (Exception e) {
            log.error("Error occurred while dispatching verification email to {}: {}", userEmail, e.getMessage());
        }
    }

    private String buildHtmlContent(String otp, String text) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'><title>ShopSphere Verification</title></head>" +
                "<body style='margin:0;padding:0;background-color:#f4f6f8;font-family:Arial,sans-serif;'>" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background-color:#f4f6f8;padding:40px 0;'>" +
                "<tr><td align='center'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,0.08);'>" +
                "<tr><td style='background:#0f766e;padding:25px;text-align:center;color:#ffffff;'>" +
                "<h1 style='margin:0;font-size:28px;'>ShopSphere</h1>" +
                "<p style='margin-top:8px;font-size:15px;'>Secure Account Verification</p>" +
                "</td></tr>" +
                "<tr><td style='padding:35px;'>" +
                "<h2 style='margin-top:0;color:#1e293b;'>Verify Your Account</h2>" +
                "<p style='font-size:16px;color:#475569;line-height:1.7;'>" + text + "</p>" +
                "<div style='margin:30px 0;text-align:center;'>" +
                "<span style='display:inline-block;background:#f1f5f9;border:2px dashed #0f766e;padding:18px 35px;font-size:34px;font-weight:bold;letter-spacing:8px;color:#0f766e;border-radius:10px;'>" +
                otp +
                "</span>" +
                "</div>" +
                "<p style='font-size:15px;color:#475569;line-height:1.7;'>This verification code is valid for <strong>10 minutes</strong>.</p>" +
                "<p style='font-size:15px;color:#475569;line-height:1.7;'>For your security, never share this OTP with anyone. ShopSphere will never ask you for your verification code.</p>" +
                "<hr style='border:none;border-top:1px solid #e2e8f0;margin:30px 0;'/>" +
                "<p style='font-size:14px;color:#64748b;'>If you did not request this verification, you can safely ignore this email.</p>" +
                "<p style='margin-top:35px;font-size:15px;color:#334155;'>Regards,<br><strong>ShopSphere Team</strong></p>" +
                "</td></tr>" +
                "<tr><td style='background:#f8fafc;padding:18px;text-align:center;font-size:13px;color:#64748b;'>© 2026 ShopSphere. All rights reserved.</td></tr>" +
                "</table></td></tr></table></body></html>";
    }
}