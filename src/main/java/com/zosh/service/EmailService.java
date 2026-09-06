package com.zosh.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zosh.exceptions.EmailDeliveryException;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.email.brevo.api-key:}")
    private String brevoApiKey;

    @Value("${app.email.brevo.from-email:noreply@shopsphere.com}")
    private String brevoFromEmail;

    @Value("${app.email.brevo.from-name:ShopSphere}")
    private String brevoFromName;

    private HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    void setBrevoApiKey(String brevoApiKey) {
        this.brevoApiKey = brevoApiKey;
    }

    void setBrevoFromEmail(String brevoFromEmail) {
        this.brevoFromEmail = brevoFromEmail;
    }

    void setBrevoFromName(String brevoFromName) {
        this.brevoFromName = brevoFromName;
    }

    /**
     * Sends a verification OTP email via the Brevo HTTPS Transactional Email API.
     * Operates over standard HTTPS (port 443), avoiding SMTP port blocks on cloud hosts like Render.
     *
     * Security constraints:
     * - OTP values and API keys are NEVER printed in log output.
     * - If delivery fails, throws an EmailDeliveryException to prevent returning a false success response to callers.
     *
     * @param userEmail recipient email address
     * @param otp       6-digit verification code
     * @param subject   email subject line
     * @param text      explanatory text for the email body
     * @throws EmailDeliveryException if Brevo API rejects the request or dispatch fails
     */
    public void sendVerificationOtpEmail(
            String userEmail,
            String otp,
            String subject,
            String text) {

        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            log.error("Brevo API key is not configured. Set BREVO_API_KEY in environment variables to enable email delivery.");
            throw new EmailDeliveryException("Email service is not configured. Please configure BREVO_API_KEY in environment variables.");
        }

        try {
            String htmlContent = buildHtmlContent(otp, text);

            ObjectNode body = objectMapper.createObjectNode();

            // Sender object: { "name": "...", "email": "..." }
            ObjectNode sender = body.putObject("sender");
            sender.put("name", (brevoFromName != null && !brevoFromName.isBlank()) ? brevoFromName.trim() : "ShopSphere");
            sender.put("email", (brevoFromEmail != null && !brevoFromEmail.isBlank()) ? brevoFromEmail.trim() : "noreply@shopsphere.com");

            // Recipient list: [ { "email": "..." } ]
            ArrayNode toArray = body.putArray("to");
            ObjectNode recipient = toArray.addObject();
            recipient.put("email", userEmail != null ? userEmail.trim() : "");

            body.put("subject", subject);
            body.put("htmlContent", htmlContent);

            String requestBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BREVO_API_URL))
                    .header("api-key", brevoApiKey.trim())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            if (statusCode == 201) {
                log.info("OTP verification email successfully dispatched via Brevo HTTPS API to {}", userEmail);
                return;
            }

            String responseBody = response.body();
            String errorDetail = extractErrorMessage(responseBody);

            if (statusCode == 400) {
                log.error("Brevo API rejected email request for recipient {}: HTTP 400 - {}", userEmail, errorDetail);
                throw new EmailDeliveryException("Failed to send verification email: Invalid email request. Ensure BREVO_FROM_EMAIL is a verified sender in Brevo: " + errorDetail);
            } else if (statusCode == 401 || statusCode == 403) {
                log.error("Brevo API authentication failure for recipient {}: HTTP {}", userEmail, statusCode);
                throw new EmailDeliveryException("Failed to send verification email: Brevo API authentication failed. Check BREVO_API_KEY.");
            } else if (statusCode == 429) {
                log.error("Brevo API rate limit exceeded for recipient {}: HTTP 429", userEmail);
                throw new EmailDeliveryException("Failed to send verification email: Brevo rate limit exceeded. Please try again later.");
            } else {
                log.error("Brevo API returned non-success response for recipient {}: HTTP {} - {}", userEmail, statusCode, errorDetail);
                throw new EmailDeliveryException("Failed to send verification email: Brevo service returned HTTP " + statusCode);
            }

        } catch (EmailDeliveryException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Email dispatch to {} was interrupted", userEmail);
            throw new EmailDeliveryException("Failed to send verification email: Operation was interrupted", e);
        } catch (IOException e) {
            log.error("Network error while dispatching email via Brevo HTTPS API to {}: {}", userEmail, e.getMessage());
            throw new EmailDeliveryException("Failed to send verification email: Network connection failed", e);
        } catch (Exception e) {
            log.error("Unexpected error occurred while dispatching email via Brevo to {}: {}", userEmail, e.getMessage());
            throw new EmailDeliveryException("Failed to send verification email: Unexpected error occurred", e);
        }
    }

    private String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "No response body returned by email service";
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("message") && !root.get("message").asText().isBlank()) {
                return root.get("message").asText();
            }
            if (root.has("code") && !root.get("code").asText().isBlank()) {
                return root.get("code").asText();
            }
        } catch (Exception ignored) {
            // If body is not JSON or parsing fails, return a truncated preview
        }
        return responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody;
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