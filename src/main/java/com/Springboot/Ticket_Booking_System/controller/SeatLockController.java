package com.Springboot.Ticket_Booking_System.controller;

import com.Springboot.Ticket_Booking_System.dto.LockRequest;
import com.Springboot.Ticket_Booking_System.model.BookedSeat;
import com.Springboot.Ticket_Booking_System.model.Show;
import com.Springboot.Ticket_Booking_System.repository.BookedSeatRepository;
import com.Springboot.Ticket_Booking_System.repository.ShowRepository;
import com.Springboot.Ticket_Booking_System.service.SeatLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles the temporary seat-lock protocol.
 *
 * POST /api/seats/lock   — update (or clear) the lock set for a session
 * GET  /api/seats/status — return booked + lock status so the UI can paint seats
 * GET  /api/seats/booked — backward-compat alias (no lock data)
 */
@RestController
@RequestMapping("/api/seats")
public class SeatLockController {

    @Autowired private SeatLockService      seatLockService;
    @Autowired private ShowRepository       showRepository;
    @Autowired private BookedSeatRepository bookedSeatRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/seats/lock
    //
    // Replaces the caller's lock set with the supplied seats list.
    // Send seats=[] to release all locks (page unmount / payment failure).
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/lock")
    public ResponseEntity<Map<String, Object>> lockSeats(@RequestBody LockRequest req) {
        try {
            if (req.getSessionId() == null || req.getSessionId().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "sessionId is required"));
            }

            Show show = getOrCreateShow(req.getMovieId(), req.getDate(), req.getTime());
            List<String> seats = req.getSeats() == null ? List.of() : req.getSeats();

            seatLockService.updateLocks(show.getId(), seats, req.getSessionId());

            return ResponseEntity.ok(Map.of("locked", seats.size(), "showId", show.getId()));

        } catch (Exception e) {
            System.err.println("⚠️ /api/seats/lock error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Could not update seat locks: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/seats/status
    //
    // Returns three lists that the frontend merges to paint the seat grid:
    //   bookedSeats    — permanently booked (blocked)
    //   lockedByOthers — held by another session right now (unavailable)
    //   myLocks        — held by this session (treated as "selected")
    //
    // Returns empty lists (200) rather than 500 on any error so the UI
    // degrades gracefully instead of breaking the polling loop.
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSeatStatus(
            @RequestParam Integer movieId,
            @RequestParam String  date,
            @RequestParam String  time,
            @RequestParam String  sessionId) {

        try {
            LocalDate showDate = LocalDate.parse(date);

            // findFirst tolerates duplicate show rows — if the show was accidentally
            // created twice (before the UNIQUE constraint was in place), the old
            // findBy... would throw IncorrectResultSizeDataAccessException -> 500.
            Optional<Show> showOpt = showRepository
                .findFirstByMovieIdAndShowDateAndShowTime(movieId, showDate, time);

            if (showOpt.isEmpty()) {
                // No show row yet for this slot — all seats are free
                return ResponseEntity.ok(emptyStatus());
            }

            Long showId = showOpt.get().getId();

            List<String> bookedSeats = bookedSeatRepository.findByShowId(showId)
                .stream()
                .map(BookedSeat::getSeatId)
                .collect(Collectors.toList());

            Map<String, List<String>> lockStatus = seatLockService.getSeatStatus(showId, sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("bookedSeats",    bookedSeats);
            response.put("lockedByOthers", lockStatus.get("lockedByOthers"));
            response.put("myLocks",        lockStatus.get("myLocks"));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("⚠️ /api/seats/status error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            // Return empty-but-valid 200 so the polling loop keeps running
            // and doesn't flood logs with 500s. Real error is printed above.
            return ResponseEntity.ok(emptyStatus());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/seats/booked  (backward-compat — used by older code)
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/booked")
    public ResponseEntity<List<String>> getBookedSeats(
            @RequestParam Integer movieId,
            @RequestParam String  date,
            @RequestParam String  time) {
        try {
            LocalDate showDate = LocalDate.parse(date);
            Optional<Show> showOpt = showRepository
                .findFirstByMovieIdAndShowDateAndShowTime(movieId, showDate, time);

            if (showOpt.isEmpty()) return ResponseEntity.ok(List.of());

            List<String> seats = bookedSeatRepository.findByShowId(showOpt.get().getId())
                .stream()
                .map(BookedSeat::getSeatId)
                .collect(Collectors.toList());

            return ResponseEntity.ok(seats);
        } catch (Exception e) {
            System.err.println("⚠️ /api/seats/booked error: " + e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper — find or lazily create the Show row
    // ─────────────────────────────────────────────────────────────────────────
    private Show getOrCreateShow(Integer movieId, String date, String time) {
        LocalDate showDate = LocalDate.parse(date);
        return showRepository
            .findFirstByMovieIdAndShowDateAndShowTime(movieId, showDate, time)
            .orElseGet(() -> {
                Show s = new Show();
                s.setMovieId(movieId);
                s.setShowDate(showDate);
                s.setShowTime(time);
                return showRepository.save(s);
            });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper — empty status map (no show row yet, or error fallback)
    // ─────────────────────────────────────────────────────────────────────────
    private Map<String, Object> emptyStatus() {
        Map<String, Object> m = new HashMap<>();
        m.put("bookedSeats",    List.of());
        m.put("lockedByOthers", List.of());
        m.put("myLocks",        List.of());
        return m;
    }
}