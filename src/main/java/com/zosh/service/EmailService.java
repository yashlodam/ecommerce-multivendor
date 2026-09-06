package com.zosh.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender javaMailSender;

    public void sendVerificationOtpEmail(
            String userEmail,
            String otp,
            String subject,
            String text) {

        try {

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, true, "UTF-8");

            String htmlContent =
                    "<!DOCTYPE html>" +
                    "<html>" +
                    "<head>" +
                    "<meta charset='UTF-8'>" +
                    "<title>ShopSphere Verification</title>" +
                    "</head>" +

                    "<body style='margin:0;padding:0;background-color:#f4f6f8;font-family:Arial,sans-serif;'>" +

                    "<table width='100%' cellpadding='0' cellspacing='0' style='background-color:#f4f6f8;padding:40px 0;'>" +
                    "<tr>" +
                    "<td align='center'>" +

                    "<table width='600' cellpadding='0' cellspacing='0' style='background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,0.08);'>" +

                    "<tr>" +
                    "<td style='background:#0f766e;padding:25px;text-align:center;color:#ffffff;'>" +
                    "<h1 style='margin:0;font-size:28px;'>ShopSphere</h1>" +
                    "<p style='margin-top:8px;font-size:15px;'>Secure Account Verification</p>" +
                    "</td>" +
                    "</tr>" +

                    "<tr>" +
                    "<td style='padding:35px;'>" +

                    "<h2 style='margin-top:0;color:#1e293b;'>Verify Your Account</h2>" +

                    "<p style='font-size:16px;color:#475569;line-height:1.7;'>" +
                    text +
                    "</p>" +

                    "<div style='margin:30px 0;text-align:center;'>" +
                    "<span style='display:inline-block;background:#f1f5f9;border:2px dashed #0f766e;padding:18px 35px;font-size:34px;font-weight:bold;letter-spacing:8px;color:#0f766e;border-radius:10px;'>" +
                    otp +
                    "</span>" +
                    "</div>" +

                    "<p style='font-size:15px;color:#475569;line-height:1.7;'>" +
                    "This verification code is valid for <strong>10 minutes</strong>." +
                    "</p>" +

                    "<p style='font-size:15px;color:#475569;line-height:1.7;'>" +
                    "For your security, never share this OTP with anyone. ShopSphere will never ask you for your verification code." +
                    "</p>" +

                    "<hr style='border:none;border-top:1px solid #e2e8f0;margin:30px 0;'/>" +

                    "<p style='font-size:14px;color:#64748b;'>" +
                    "If you did not request this verification, you can safely ignore this email." +
                    "</p>" +

                    "<p style='margin-top:35px;font-size:15px;color:#334155;'>" +
                    "Regards,<br>" +
                    "<strong>ShopSphere Team</strong>" +
                    "</p>" +

                    "</td>" +
                    "</tr>" +

                    "<tr>" +
                    "<td style='background:#f8fafc;padding:18px;text-align:center;font-size:13px;color:#64748b;'>" +
                    "© 2026 ShopSphere. All rights reserved." +
                    "</td>" +
                    "</tr>" +

                    "</table>" +

                    "</td>" +
                    "</tr>" +
                    "</table>" +

                    "</body>" +
                    "</html>";

            helper.setTo(userEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);

            log.info("OTP email sent successfully to {}", userEmail);

        } catch (Exception e) {

            log.error("Failed to send email to {}: {}", userEmail, e.getMessage(), e);

            throw new RuntimeException(
                    "Unable to send OTP email to " + userEmail, e);
        }
    }
}