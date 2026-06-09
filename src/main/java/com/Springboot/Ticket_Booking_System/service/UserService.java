package com.Springboot.Ticket_Booking_System.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class UserService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    private final String CLERK_API_URL = "https://api.clerk.com/v1/users/";
    private final String CLERK_SECRET_KEY = "sk_your_clerk_secret_key"; // Replace with your Clerk secret key
    
    public ClerkUser getUserDetails(String clerkUserId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + CLERK_SECRET_KEY);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<ClerkUser> response = restTemplate.exchange(
                CLERK_API_URL + clerkUserId,
                HttpMethod.GET,
                entity,
                ClerkUser.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Failed to fetch user details: " + e.getMessage());
            return null;
        }
    }
    
    // Inner class for Clerk user response
    public static class ClerkUser {
        private String id;
        private String firstName;
        private String lastName;
        private String emailAddress;
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getEmailAddress() { return emailAddress; }
        public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }
        
        public String getFullName() {
            return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
        }
    }
}