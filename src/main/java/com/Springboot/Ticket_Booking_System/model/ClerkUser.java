package com.Springboot.Ticket_Booking_System.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClerkUser(
        String id,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        @JsonProperty("email_addresses") List<EmailAddress> emailAddresses) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmailAddress(@JsonProperty("email_address") String emailAddress) {}

    public String primaryEmail() {
        if (emailAddresses == null || emailAddresses.isEmpty()) return null;
        return emailAddresses.get(0).emailAddress();
    }
}
