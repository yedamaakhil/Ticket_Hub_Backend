package com.Springboot.Ticket_Booking_System.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class UserService {

    private static final String CLERK_API_URL = "https://api.clerk.com/v1/users/";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${clerk.secret-key:}")
    private String clerkSecretKey;

    /**
     * Fetches the user's primary email from Clerk (fallback when JWT / request omit email).
     */
    public String getPrimaryEmail(String clerkUserId) {
        if (clerkUserId == null || clerkUserId.isBlank()) {
            return null;
        }
        if (clerkSecretKey == null || clerkSecretKey.isBlank()) {
            System.err.println("Clerk email lookup skipped: CLERK_SECRET_KEY is not configured");
            return null;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + clerkSecretKey.trim());
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                CLERK_API_URL + clerkUserId,
                HttpMethod.GET,
                entity,
                String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode emails = root.get("email_addresses");
            if (emails == null || !emails.isArray() || emails.isEmpty()) {
                return null;
            }

            String primaryId = root.has("primary_email_address_id")
                ? root.get("primary_email_address_id").asText(null)
                : null;

            if (primaryId != null) {
                for (JsonNode emailNode : emails) {
                    if (emailNode.has("id") && primaryId.equals(emailNode.get("id").asText())) {
                        return emailNode.get("email_address").asText(null);
                    }
                }
            }

            return emails.get(0).get("email_address").asText(null);
        } catch (Exception e) {
            System.err.println("Failed to fetch Clerk email for " + clerkUserId + ": " + e.getMessage());
            return null;
        }
    }
}