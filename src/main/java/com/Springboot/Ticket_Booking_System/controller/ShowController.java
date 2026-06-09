package com.Springboot.Ticket_Booking_System.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Springboot.Ticket_Booking_System.model.Show;
import com.Springboot.Ticket_Booking_System.repository.BookedSeatRepository;
import com.Springboot.Ticket_Booking_System.repository.ShowRepository;

@RestController
@RequestMapping("/api/shows")
@CrossOrigin(origins = "http://localhost:5173")
public class ShowController {

    @Autowired private ShowRepository       showRepository;
    @Autowired private BookedSeatRepository bookedSeatRepository;

    // ── GET /api/shows/active  (used by Dashboard + ListShow) ──────────────
    @GetMapping("/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveShows() {
        List<Show> shows = showRepository.findAll();

        List<Map<String, Object>> result = shows.stream().map(show -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id",                 show.getId());
            m.put("movieId",            show.getMovieId());
            m.put("showDate",           show.getShowDate() != null ? show.getShowDate().toString() : null);
            m.put("showTime",           show.getShowTime());
            // Extra fields stored on Show (if you extend the entity later)
            m.put("theaterName",        show.getTheaterName());
            m.put("screenName",         show.getScreenName());
            m.put("language",           show.getLanguage());
            m.put("totalSeats",         show.getTotalSeats() != null ? show.getTotalSeats() : 100);
            m.put("ticketPrice",        show.getTicketPrice() != null ? show.getTicketPrice() : 0);
            // Booked seats count from BookedSeat table
            long bookedCount = bookedSeatRepository.countByShowId(show.getId());
            m.put("occupiedSeatsCount", bookedCount);
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ── POST /api/shows  (called by AddShow.jsx) ────────────────────────────
    // NOTE: You need to add theaterName, screenName, language, totalSeats,
    //       ticketPrice fields to your Show entity (see comments below).
    //       Minimal version works with existing Show entity too.
    @PostMapping
    public ResponseEntity<Map<String, Object>> createShow(
            @RequestBody Map<String, Object> body) {
        try {
            Show show = new Show();
            show.setMovieId(Integer.valueOf(String.valueOf(body.get("movieId"))));
            show.setShowDate(LocalDate.parse(String.valueOf(body.get("showDate"))));
            show.setShowTime(String.valueOf(body.get("showTime")));

            // Optional fields — set them if your Show entity has them
            // (Add these fields to Show.java to enable full functionality)
            if (body.containsKey("theaterName") && show.getClass().getDeclaredFields() != null) {
                try { show.setTheaterName(String.valueOf(body.get("theaterName"))); } catch (Exception ignored) {}
            }
            if (body.containsKey("screenName")) {
                try { show.setScreenName(String.valueOf(body.get("screenName"))); } catch (Exception ignored) {}
            }
            if (body.containsKey("language")) {
                try { show.setLanguage(String.valueOf(body.get("language"))); } catch (Exception ignored) {}
            }
            if (body.containsKey("totalSeats")) {
                try { show.setTotalSeats(Integer.valueOf(String.valueOf(body.get("totalSeats")))); } catch (Exception ignored) {}
            }
            if (body.containsKey("showPrice")) {
                try { show.setTicketPrice(Integer.valueOf(String.valueOf(body.get("showPrice")))); } catch (Exception ignored) {}
            }

            Show saved = showRepository.save(show);

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("id",        saved.getId());
            response.put("movieId",   saved.getMovieId());
            response.put("showDate",  saved.getShowDate().toString());
            response.put("showTime",  saved.getShowTime());
            response.put("status",    "created");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── DELETE /api/shows/{id}  (called by ListShow.jsx) ───────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteShow(@PathVariable Long id) {
        if (!showRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        showRepository.deleteById(id);
        return ResponseEntity.ok("Show deleted successfully");
    }

    // ── GET /api/shows/by-date  ─────────────────────────────────────────────
    @GetMapping("/by-date")
    public ResponseEntity<List<Show>> getShowsByDate(@RequestParam String date) {
        LocalDate showDate = LocalDate.parse(date);
        return ResponseEntity.ok(showRepository.findByShowDate(showDate));
    }
}