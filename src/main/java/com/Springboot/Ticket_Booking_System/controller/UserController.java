package com.Springboot.Ticket_Booking_System.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Springboot.Ticket_Booking_System.dto.UserDTO;
import com.Springboot.Ticket_Booking_System.model.Booking;
import com.Springboot.Ticket_Booking_System.repository.BookingRepository;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    @Autowired
    private BookingRepository bookingRepository;

    @GetMapping
    public List<UserDTO> getAllUsers() {
        return bookingRepository.findAll().stream()
                .filter(b -> b.getClerkUserId() != null)
                .map(Booking::getClerkUserId)
                .distinct()
                .map(id -> new UserDTO(null, id, null, "USER"))  // name = clerkId, email = null
                .collect(Collectors.toList());
    }
}