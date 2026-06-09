package com.Springboot.Ticket_Booking_System.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.Springboot.Ticket_Booking_System.service.SeatService;

@RestController
@RequestMapping("/api/seats")
@CrossOrigin(origins = "http://localhost:5173")
public class SeatController {

    @Autowired
    private SeatService seatService;

    @GetMapping("/booked")
    public ResponseEntity<List<String>> getBookedSeats(
        @RequestParam Integer movieId,
        @RequestParam String date,
        @RequestParam String time
    ) {
        List<String> seats = seatService.getBookedSeats(movieId, date, time);
        return ResponseEntity.ok(seats);
    }
}