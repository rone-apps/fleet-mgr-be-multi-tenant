package com.taxi.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class ContactController {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String emailFrom;

    @Value("${app.contact.email:info@smartfleets.ai}")
    private String contactEmail;

    @PostMapping("/contact")
    public ResponseEntity<?> submitContactForm(@RequestBody Map<String, String> body) {
        try {
            String firstName = body.getOrDefault("firstName", "").trim();
            String lastName = body.getOrDefault("lastName", "").trim();
            String email = body.getOrDefault("email", "").trim();
            String company = body.getOrDefault("company", "").trim();
            String fleetSize = body.getOrDefault("fleetSize", "").trim();
            String message = body.getOrDefault("message", "").trim();

            if (firstName.isEmpty() || email.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "First name and email are required"));
            }

            String fullName = (firstName + " " + lastName).trim();

            StringBuilder text = new StringBuilder();
            text.append("New contact form submission from Smart Fleets website\n\n");
            text.append("Name: ").append(fullName).append("\n");
            text.append("Email: ").append(email).append("\n");
            if (!company.isEmpty()) text.append("Company: ").append(company).append("\n");
            if (!fleetSize.isEmpty()) text.append("Fleet Size: ").append(fleetSize).append("\n");
            if (!message.isEmpty()) text.append("\nMessage:\n").append(message).append("\n");

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(emailFrom);
            mailMessage.setTo(contactEmail);
            mailMessage.setReplyTo(email);
            mailMessage.setSubject("Smart Fleets - Contact from " + fullName + (company.isEmpty() ? "" : " (" + company + ")"));
            mailMessage.setText(text.toString());

            mailSender.send(mailMessage);
            log.info("Contact form submitted: {} ({}) from {}", fullName, email, company);

            return ResponseEntity.ok(Map.of("message", "Thank you! We'll be in touch shortly."));
        } catch (Exception e) {
            log.error("Failed to process contact form", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to send message. Please email us at info@smartfleets.ai"));
        }
    }
}
