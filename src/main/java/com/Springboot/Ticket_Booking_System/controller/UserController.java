package com.Springboot.Ticket_Booking_System.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import com.Springboot.Ticket_Booking_System.dto.UserDTO;
import com.Springboot.Ticket_Booking_System.model.Booking;
import com.Springboot.Ticket_Booking_System.model.ClerkUser;
import com.Springboot.Ticket_Booking_System.repository.BookingRepository;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    @Autowired
    private BookingRepository bookingRepository;

    @Value("${clerk.secret-key}")
    private String clerkSecretKey;

    private final RestClient restClient = RestClient.create();

    @GetMapping
    public List<UserDTO> getAllUsers() {
        // 1. Get distinct Clerk user IDs from all bookings
        List<String> userIds = bookingRepository.findAll().stream()
                .map(Booking::getClerkUserId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return List.of();
        }

        // 2. Build query string: ?user_id=id1&user_id=id2... (Clerk accepts up to 100)
        StringBuilder url = new StringBuilder("https://api.clerk.com/v1/users?limit=100");
        for (String id : userIds.stream().limit(100).collect(Collectors.toList())) {
            url.append("&user_id=").append(id);
        }

        // 3. Call Clerk Backend API
        ClerkUser[] clerkUsers = restClient.get()
                .uri(url.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clerkSecretKey)
                .retrieve()
                .body(ClerkUser[].class);

        if (clerkUsers == null) {
            return List.of();
        }

        // 4. Map Clerk response -> your UserDTO
        return java.util.Arrays.stream(clerkUsers)
                .map(cu -> new UserDTO(
                        null,
                        buildName(cu),
                        cu.primaryEmail(),
                        "USER"))
                .collect(Collectors.toList());
    }

    private String buildName(ClerkUser cu) {
        String first = cu.firstName() != null ? cu.firstName() : "";
        String last = cu.lastName() != null ? cu.lastName() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? null : full;
    }
}