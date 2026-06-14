package com.zosh.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

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
                    "<html>" +
                    "<body>" +
                    "<h2>Email Verification</h2>" +
                    "<p>" + text + "</p>" +
                    "<h1 style='color:blue;'>" + otp + "</h1>" +
                    "<p>This OTP is valid for 10 minutes.</p>" +
                    "<p>Please do not share this OTP with anyone.</p>" +
                    "</body>" +
                    "</html>";

            helper.setTo(userEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);

            System.out.println("Email sent successfully to " + userEmail);

        } catch (Exception e) {

            System.err.println("Failed to send email: " + e.getMessage());

            throw new RuntimeException(
                    "Unable to send OTP email to " + userEmail,
                    e);
        }
    }
}