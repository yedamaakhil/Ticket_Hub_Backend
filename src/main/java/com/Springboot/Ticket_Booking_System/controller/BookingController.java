package com.Springboot.Ticket_Booking_System.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Springboot.Ticket_Booking_System.dto.BookingRequest;
import com.Springboot.Ticket_Booking_System.model.Booking;
import com.Springboot.Ticket_Booking_System.service.BookingService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    // POST — create booking
    @PostMapping
    public ResponseEntity<Map<String, Object>> createBooking(
        @RequestBody BookingRequest request,
        HttpServletRequest httpRequest
    ) {
        String clerkUserId = (String) httpRequest.getAttribute("clerkUserId");

        if (clerkUserId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized — please sign in again"));
        }

        Map<String, Object> result = bookingService.createBooking(request, clerkUserId);
        return ResponseEntity.ok(result);
    }

    // GET — user bookings
    @GetMapping("/user/{clerkUserId}")
    public ResponseEntity<List<Booking>> getUserBookings(
        @PathVariable String clerkUserId
    ) {
        return ResponseEntity.ok(bookingService.getUserBookings(clerkUserId));
    }

    // PUT — cancel booking
    @PutMapping("/{id}/cancel")
    public ResponseEntity<String> cancelBooking(@PathVariable Long id) {
        bookingService.cancelBooking(id);
        return ResponseEntity.ok("Booking cancelled successfully");
    }
    
    
//    ------------------------------------------ Admin -----------------------------------------------//
    
 // Add these methods to your existing BookingController.java

	 // Get all bookings (for admin dashboard)
	 @GetMapping("/all")
	 public ResponseEntity<List<Booking>> getAllBookings() {
	     List<Booking> bookings = bookingService.getAllBookings();
	     return ResponseEntity.ok(bookings);
	 }
	
	 // Get booking statistics for dashboard
	 @GetMapping("/stats")
	 public ResponseEntity<Map<String, Object>> getBookingStats() {
	     Map<String, Object> stats = bookingService.getBookingStatistics();
	     return ResponseEntity.ok(stats);
	 }
    
    
}