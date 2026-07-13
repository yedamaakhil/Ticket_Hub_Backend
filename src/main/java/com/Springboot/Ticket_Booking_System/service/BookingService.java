package com.Springboot.Ticket_Booking_System.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.Springboot.Ticket_Booking_System.dto.BookingRequest;
import com.Springboot.Ticket_Booking_System.exception.SeatUnavailableException;
import com.Springboot.Ticket_Booking_System.model.Booking;
import com.Springboot.Ticket_Booking_System.model.BookedSeat;
import com.Springboot.Ticket_Booking_System.model.Payment;
import com.Springboot.Ticket_Booking_System.model.Show;
import com.Springboot.Ticket_Booking_System.repository.BookedSeatRepository;
import com.Springboot.Ticket_Booking_System.repository.BookingRepository;
import com.Springboot.Ticket_Booking_System.repository.PaymentRepository;
import com.Springboot.Ticket_Booking_System.repository.ShowRepository;

@Service
public class BookingService {

    @Autowired private ShowRepository       showRepository;
    @Autowired private BookedSeatRepository bookedSeatRepository;
    @Autowired private BookingRepository    bookingRepository;
    @Autowired private PaymentRepository    paymentRepository;
    @Autowired private EmailService         emailService;
    @Autowired private SeatLockService      seatLockService;

    @Transactional
    public Map<String, Object> createBooking(BookingRequest req, String clerkUserId) {

        System.out.println("📝 Creating booking for user: " + clerkUserId);
        System.out.println("📧 User email from request: " + req.getUserEmail());
        System.out.println("💳 Razorpay Payment ID: " + req.getRazorpayPaymentId());

        validateBookingRequest(req);

        LocalDate date = LocalDate.parse(req.getShowDate());

        // ── Find or create show ───────────────────────────────────────────────
        // Uses findFirst to be resilient if duplicate show rows exist in the DB
        // (can happen when both lockSeats and createBooking call orElseGet→save).
        Show show = showRepository
            .findFirstByMovieIdAndShowDateAndShowTime(req.getMovieId(), date, req.getShowTime())
            .orElseGet(() -> {
                Show s = new Show();
                s.setMovieId(req.getMovieId());
                s.setShowDate(date);
                s.setShowTime(req.getShowTime());
                return showRepository.save(s);
            });

        // Pre-check seats (fast path — DB unique constraint is the real guard)
        for (String seatId : req.getSeats()) {
            if (bookedSeatRepository.existsByShowIdAndSeatId(show.getId(), seatId)) {
                throw new SeatUnavailableException("Seat " + seatId + " is already booked");
            }
        }

        String bookingRef = "BK" + System.currentTimeMillis();

        Booking booking = new Booking();
        booking.setBookingRef(bookingRef);
        booking.setShowId(show.getId());
        booking.setClerkUserId(clerkUserId);
        booking.setTotalPrice(req.getTotalPrice());
        booking.setStatus("CONFIRMED");
        booking.setMovieTitle(truncate(req.getMovieTitle(), 500));
        booking.setMoviePosterPath(truncate(req.getMoviePosterPath(), 2000));
        booking.setMovieGenres(truncate(req.getMovieGenres(), 500));
        booking.setMovieRuntime(req.getMovieRuntime());
        booking.setMovieLanguage(truncate(req.getMovieLanguage(), 50));
        booking.setShowDate(truncate(req.getShowDate(), 20));
        booking.setShowTime(truncate(req.getShowTime(), 50));
        booking.setSeats(req.getSeats());
        booking.setTheaterName(truncate(req.getTheaterName(), 255));
        booking.setScreenName(truncate(req.getScreenName(), 100));
        booking.setTransactionId(truncate(req.getRazorpayPaymentId(), 100));

        String userEmail = req.getUserEmail();
        if (userEmail != null && !userEmail.isBlank()) {
            booking.setUserEmail(truncate(userEmail.trim(), 255));
        }

        bookingRepository.save(booking);
        System.out.println("✅ Booking saved — ID: " + booking.getId() + ", Ref: " + bookingRef);

        // Save booked seats — DB unique constraint catches concurrent double-bookings
        try {
            for (String seatId : req.getSeats()) {
                BookedSeat bs = new BookedSeat();
                bs.setShowId(show.getId());
                bs.setSeatId(seatId);
                bs.setTier(getTier(seatId));
                bs.setPrice(resolveSeatPrice(req, seatId));
                bs.setClerkUserId(clerkUserId);
                bs.setBookingId(booking.getId());
                bookedSeatRepository.save(bs);
                bookedSeatRepository.flush(); // push constraint check to now
            }
        } catch (DataIntegrityViolationException e) {
            System.err.println("⚠️ Concurrent booking detected: " + e.getMessage());
            throw new SeatUnavailableException(
                "One or more seats were just booked by someone else. Please pick different seats.");
        }

        System.out.println("✅ Booked " + req.getSeats().size() + " seat(s)");

        // ── Release seat locks ────────────────────────────────────────────────
        // CRITICAL: wrapped in try/catch so a lock-release failure never rolls
        // back an already-confirmed booking. Locks also auto-expire after 10 min.
        String sessionId = req.getSessionId();
        if (sessionId != null && !sessionId.isBlank()) {
            try {
                seatLockService.releaseLocks(show.getId(), sessionId);
                System.out.println("🔓 Released seat locks for session: " + sessionId);
            } catch (Exception lockEx) {
                System.err.println("⚠️ Could not release seat locks (non-fatal, will expire): "
                    + lockEx.getMessage());
            }
        }

        // ── Save payment record ───────────────────────────────────────────────
        Payment payment = new Payment();
        payment.setBookingId(booking.getId());
        payment.setAmount(req.getTotalPrice());
        payment.setPaymentMethod(
            req.getPaymentMethod() != null && !req.getPaymentMethod().isBlank()
                ? req.getPaymentMethod() : "RAZORPAY");
        payment.setStatus("SUCCESS");
        payment.setTransactionId(truncate(req.getRazorpayPaymentId(), 100));
        paymentRepository.save(payment);
        System.out.println("✅ Payment saved — TxID: " + payment.getTransactionId());

        Map<String, Object> response = new HashMap<>();
        response.put("id",            booking.getId());
        response.put("bookingRef",    bookingRef);
        response.put("status",        "CONFIRMED");
        response.put("transactionId", req.getRazorpayPaymentId());
        response.put("emailSent",     false);

        if (userEmail != null && !userEmail.trim().isEmpty()) {
            Booking savedBooking = booking;
            String  recipient    = userEmail.trim();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    boolean sent = emailService.sendBookingConfirmation(savedBooking, recipient);
                    response.put("emailSent", sent);
                    if (!sent) System.err.println("⚠️ Ticket email NOT sent to: " + recipient);
                }
            });
        } else {
            System.err.println("⚠️ No user email — confirmation email skipped");
        }

        System.out.println("🎉 Booking completed successfully!");
        return response;
    }

    public Map<String, Object> resendConfirmationEmail(Long bookingId) {
        Map<String, Object> result = new HashMap<>();
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            result.put("emailSent", false);
            result.put("emailError", "Booking not found");
            return result;
        }
        String recipient = booking.getUserEmail();
        if (recipient == null || recipient.isBlank()) {
            result.put("emailSent", false);
            result.put("emailError", "No email address stored for this booking");
            return result;
        }
        boolean sent = emailService.sendBookingConfirmation(booking, recipient);
        result.put("emailSent", sent);
        if (!sent) result.put("emailError", "Failed to send — check RESEND_API_KEY");
        return result;
    }

    public List<Booking> getUserBookings(String clerkUserId) {
        return bookingRepository.findByClerkUserId(clerkUserId);
    }

    @Transactional
    public void cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Booking not found: " + id));
        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Map<String, Object> getBookingStatistics() {
        List<Booking> bookings = bookingRepository.findAll();
        long   total     = bookings.size();
        double revenue   = bookings.stream().filter(b -> "CONFIRMED".equals(b.getStatus()))
                                            .mapToDouble(Booking::getTotalPrice).sum();
        long   users     = bookings.stream().map(Booking::getClerkUserId).distinct().count();
        long   confirmed = bookings.stream().filter(b -> "CONFIRMED".equals(b.getStatus())).count();
        long   cancelled = bookings.stream().filter(b -> "CANCELLED".equals(b.getStatus())).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBookings",      total);
        stats.put("totalRevenue",       revenue);
        stats.put("totalUsers",         users);
        stats.put("averageBookingValue", total > 0 ? revenue / total : 0);
        stats.put("completedBookings",  confirmed);
        stats.put("cancelledBookings",  cancelled);
        return stats;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private String getTier(String seatId) {
        char row = seatId.charAt(0);
        if (row <= 'B') return "economy";
        if (row <= 'J') return "standard";
        return "premium";
    }

    private void validateBookingRequest(BookingRequest req) {
        if (req.getMovieId()  == null) throw new IllegalArgumentException("movieId is required");
        if (req.getShowDate() == null || req.getShowDate().isBlank())  throw new IllegalArgumentException("showDate is required");
        if (req.getShowTime() == null || req.getShowTime().isBlank())  throw new IllegalArgumentException("showTime is required");
        if (req.getSeats()    == null || req.getSeats().isEmpty())     throw new IllegalArgumentException("At least one seat is required");
        if (req.getTotalPrice() == null) throw new IllegalArgumentException("totalPrice is required");
    }

    private Integer resolveSeatPrice(BookingRequest req, String seatId) {
        if (req.getSeatPrices() != null && req.getSeatPrices().get(seatId) != null)
            return req.getSeatPrices().get(seatId);
        return defaultPriceForSeat(seatId);
    }

    private Integer defaultPriceForSeat(String seatId) {
        return switch (getTier(seatId)) {
            case "economy"  -> 150;
            case "standard" -> 300;
            default         -> 500;
        };
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}