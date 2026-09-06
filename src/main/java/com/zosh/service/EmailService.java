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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender javaMailSender;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${app.email.resend.api-key:}")
    private String resendApiKey;

    @Value("${app.email.resend.from:ShopSphere <onboarding@resend.dev>}")
    private String resendFrom;

    @Value("${app.email.brevo.api-key:}")
    private String brevoApiKey;

    @Value("${app.email.brevo.from-email:noreply@shopsphere.com}")
    private String brevoFromEmail;

    @Value("${app.email.brevo.from-name:ShopSphere}")
    private String brevoFromName;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * Sends a verification OTP email asynchronously so the HTTP request
     * completes immediately (< 50ms) without waiting for SMTP or network timeouts.
     */
    @Async
    public void sendVerificationOtpEmail(
            String userEmail,
            String otp,
            String subject,
            String text) {

        String htmlContent = buildHtmlContent(otp, text);

        // 1. Resend HTTPS API (Recommended for Render Free Tier — HTTPS port 443 is never blocked)
        if (resendApiKey != null && !resendApiKey.isBlank()) {
            try {
                sendViaResend(userEmail, subject, htmlContent);
                log.info("OTP email sent successfully via Resend HTTPS API to {}", userEmail);
                return;
            } catch (Exception e) {
                log.error("Resend API delivery failed for {}: {}", userEmail, e.getMessage());
            }
        }

        // 2. Brevo HTTPS API (port 443)
        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            try {
                sendViaBrevo(userEmail, subject, htmlContent);
                log.info("OTP email sent successfully via Brevo HTTPS API to {}", userEmail);
                return;
            } catch (Exception e) {
                log.error("Brevo API delivery failed for {}: {}", userEmail, e.getMessage());
            }
        }

        // 3. JavaMailSender SMTP (port 587/465 — works locally or on paid hosting)
        if (mailUsername != null && !mailUsername.isBlank()
                && mailPassword != null && !mailPassword.isBlank()
                && javaMailSender != null) {
            try {
                MimeMessage mimeMessage = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setTo(userEmail);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);

                javaMailSender.send(mimeMessage);
                log.info("OTP email sent successfully via SMTP to {}", userEmail);
                return;
            } catch (Exception e) {
                log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                log.error("SMTP DELIVERY FAILED for {}: {}", userEmail, e.getMessage());
                log.error("[RENDER NOTICE] Render Free Tier blocks outbound SMTP ports (25, 465, 587).");
                log.error("FALLBACK: LOGIN / SIGNUP OTP FOR [{}] IS: >>> {} <<<", userEmail, otp);
                log.error("To send real emails on Render Free Tier, get a free key at https://resend.com and add RESEND_API_KEY in Render.");
                log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                return;
            }
        }

        // 4. Fallback: No email credentials configured
        log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.warn("[EMAIL SERVICE NOTICE] No email provider configured.");
        log.warn("LOGIN / SIGNUP OTP FOR [{}] IS: >>> {} <<<", userEmail, otp);
        log.warn("To receive emails in your inbox, set RESEND_API_KEY in Render Environment Variables (free at https://resend.com).");
        log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void sendViaResend(String toEmail, String subject, String html) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("from", resendFrom);
        ArrayNode toArray = body.putArray("to");
        toArray.add(toEmail);
        body.put("subject", subject);
        body.put("html", html);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer " + resendApiKey.trim())
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("Resend API HTTP " + response.statusCode() + ": " + response.body());
        }
    }

    private void sendViaBrevo(String toEmail, String subject, String html) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        ObjectNode sender = body.putObject("sender");
        sender.put("name", brevoFromName);
        sender.put("email", brevoFromEmail);
        ArrayNode toArray = body.putArray("to");
        ObjectNode recipient = toArray.addObject();
        recipient.put("email", toEmail);
        body.put("subject", subject);
        body.put("htmlContent", html);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                .header("api-key", brevoApiKey.trim())
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("Brevo API HTTP " + response.statusCode() + ": " + response.body());
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