package com.waymate.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class BrevoEmailService {

    private final WebClient webClient;

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    @Value("${brevo.sender.name}")
    private String senderName;

    public BrevoEmailService(WebClient webClient) {
        this.webClient = webClient;
    }

    // Reusable method for sending emails through Brevo
    public void sendEmail(String recipient, String subject, String htmlContent) {

        //send the HTTP POST request
        webClient.post()
                //target Brevo's email endpoint
                .uri("/smtp/email")
                //authenticating using Brevo API key
                .header("api-key", brevoApiKey)
                .header("Content-Type", "application/json")
                //provide the sender, recipient, subject, and email content
                .bodyValue(Map.of(
                        "sender", Map.of(
                                "name", senderName,
                                "email", senderEmail
                        ),
                        "to", List.of(
                                Map.of("email", recipient)
                        ),
                        "subject", subject,
                        "htmlContent", htmlContent
                ))
                //sends the request and retriece the response
                .retrieve()
                .bodyToMono(String.class)
                //wait for brevo's response before continuing
                .block();
    }
}