package com.Springboot.Ticket_Booking_System.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired private ShowRepository showRepository;
    @Autowired private BookedSeatRepository bookedSeatRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private EmailService emailService;

    // ─────────────────────────────────────────────────────────────────────────
    //  CREATE BOOKING (called after payment verification)
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> createBooking(BookingRequest req, String clerkUserId) {

        System.out.println("📝 Creating booking for user: " + clerkUserId);
        System.out.println("📧 User email from request: " + req.getUserEmail());
        System.out.println("💳 Razorpay Payment ID: " + req.getRazorpayPaymentId());

        validateBookingRequest(req);

        // 1. Find or create show
        LocalDate date = LocalDate.parse(req.getShowDate());
        Show show = showRepository
            .findByMovieIdAndShowDateAndShowTime(req.getMovieId(), date, req.getShowTime())
            .orElseGet(() -> {
                Show s = new Show();
                s.setMovieId(req.getMovieId());
                s.setShowDate(date);
                s.setShowTime(req.getShowTime());
                return showRepository.save(s);
            });

        // 2. Check seat availability (double-check before booking)
        for (String seatId : req.getSeats()) {
            if (bookedSeatRepository.existsByShowIdAndSeatId(show.getId(), seatId)) {
                throw new SeatUnavailableException("Seat " + seatId + " is already booked");
            }
        }

        // 3. Create booking record
        String bookingRef = "BK" + System.currentTimeMillis();

        Booking booking = new Booking();
        booking.setBookingRef(bookingRef);
        booking.setShowId(show.getId());
        booking.setClerkUserId(clerkUserId);
        booking.setTotalPrice(req.getTotalPrice());
        booking.setStatus("CONFIRMED");

        // Movie metadata (truncate for DB safety on cloud MySQL)
        booking.setMovieTitle(truncate(req.getMovieTitle(), 500));
        booking.setMoviePosterPath(truncate(req.getMoviePosterPath(), 2000));
        booking.setMovieGenres(truncate(req.getMovieGenres(), 500));
        booking.setMovieRuntime(req.getMovieRuntime());
        booking.setMovieLanguage(truncate(req.getMovieLanguage(), 50));

        // Show details
        booking.setShowDate(truncate(req.getShowDate(), 20));
        booking.setShowTime(truncate(req.getShowTime(), 50));
        booking.setSeats(req.getSeats());
        booking.setTheaterName(truncate(req.getTheaterName(), 255));
        booking.setScreenName(truncate(req.getScreenName(), 100));

        booking.setTransactionId(truncate(req.getRazorpayPaymentId(), 100));

        // ★ FIX: persist the user's email on the booking so we can resend later
        String userEmail = req.getUserEmail();
        if (userEmail != null && !userEmail.isBlank()) {
            booking.setUserEmail(truncate(userEmail.trim(), 255));
        }

        bookingRepository.save(booking);
        System.out.println("✅ Booking saved with ID: " + booking.getId() + ", Ref: " + bookingRef);

        // 4. Save each booked seat
        for (String seatId : req.getSeats()) {
            BookedSeat bs = new BookedSeat();
            bs.setShowId(show.getId());
            bs.setSeatId(seatId);
            bs.setTier(getTier(seatId));
            bs.setPrice(resolveSeatPrice(req, seatId));
            bs.setClerkUserId(clerkUserId);
            bs.setBookingId(booking.getId());
            bookedSeatRepository.save(bs);
        }
        System.out.println("✅ Booked " + req.getSeats().size() + " seats");

        // 5. Save payment record
        Payment payment = new Payment();
        payment.setBookingId(booking.getId());
        payment.setAmount(req.getTotalPrice());
        payment.setPaymentMethod(
            req.getPaymentMethod() != null && !req.getPaymentMethod().isBlank()
                ? req.getPaymentMethod()
                : "RAZORPAY"
        );
        payment.setStatus("SUCCESS");
        payment.setTransactionId(truncate(req.getRazorpayPaymentId(), 100));
        paymentRepository.save(payment);
        System.out.println("✅ Payment saved with Transaction ID: " + payment.getTransactionId());

        // ★ FIX: build the response BEFORE registering the commit-callback so the
        //         same mutable Map reference can be updated with the real email result.
        Map<String, Object> response = new HashMap<>();
        response.put("id", booking.getId());
        response.put("bookingRef", bookingRef);
        response.put("status", "CONFIRMED");
        response.put("transactionId", req.getRazorpayPaymentId());
        response.put("emailSent", false); // default — overwritten below if send succeeds

        // 6. Send confirmation email after DB commit
        if (userEmail != null && !userEmail.trim().isEmpty()) {
            Booking savedBooking = booking;
            String recipient = userEmail.trim();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    boolean sent = emailService.sendBookingConfirmation(savedBooking, recipient);
                    // Spring commits the transaction (which triggers this callback)
                    // BEFORE control returns to the controller, so mutating the same
                    // response object here is safely visible to the caller.
                    response.put("emailSent", sent);
                    if (!sent) {
                        System.err.println("⚠️ Ticket email was NOT sent to: " + recipient
                            + " — check SPRING_MAIL_* env vars on Render");
                    }
                }
            });
        } else {
            System.err.println("⚠️ No user email on booking request — confirmation email skipped");
        }

        System.out.println("🎉 Booking completed successfully!");
        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RESEND CONFIRMATION EMAIL
    // ─────────────────────────────────────────────────────────────────────────
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
        if (!sent) {
            result.put("emailError", "Failed to send — check SPRING_MAIL_* env vars on Render");
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  USER BOOKINGS
    // ─────────────────────────────────────────────────────────────────────────
    public List<Booking> getUserBookings(String clerkUserId) {
        System.out.println("📋 Fetching bookings for user: " + clerkUserId);
        List<Booking> bookings = bookingRepository.findByClerkUserId(clerkUserId);
        System.out.println("📋 Found " + bookings.size() + " bookings");
        return bookings;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CANCEL BOOKING
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public void cancelBooking(Long id) {
        System.out.println("❌ Cancelling booking with ID: " + id);
        Booking booking = bookingRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Booking not found: " + id));
        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);
        System.out.println("✅ Booking " + id + " cancelled successfully");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ADMIN — all bookings
    // ─────────────────────────────────────────────────────────────────────────
    public List<Booking> getAllBookings() {
        System.out.println("📊 Admin: Fetching all bookings");
        return bookingRepository.findAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ADMIN — booking statistics
    // ─────────────────────────────────────────────────────────────────────────
    public Map<String, Object> getBookingStatistics() {
        System.out.println("📊 Admin: Calculating booking statistics");
        List<Booking> bookings = bookingRepository.findAll();

        long totalBookings = bookings.size();
        double totalRevenue = bookings.stream()
            .filter(b -> "CONFIRMED".equals(b.getStatus()))
            .mapToDouble(Booking::getTotalPrice).sum();
        long uniqueUsers = bookings.stream()
            .map(Booking::getClerkUserId).distinct().count();
        double avgBookingValue = totalBookings > 0 ? totalRevenue / totalBookings : 0;
        long completedBookings = bookings.stream()
            .filter(b -> "CONFIRMED".equals(b.getStatus())).count();
        long cancelledBookings = bookings.stream()
            .filter(b -> "CANCELLED".equals(b.getStatus())).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBookings", totalBookings);
        stats.put("totalRevenue", totalRevenue);
        stats.put("totalUsers", uniqueUsers);
        stats.put("averageBookingValue", avgBookingValue);
        stats.put("completedBookings", completedBookings);
        stats.put("cancelledBookings", cancelledBookings);

        return stats;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private String getTier(String seatId) {
        char row = seatId.charAt(0);
        if (row <= 'B') return "economy";
        if (row <= 'J') return "standard";
        return "premium";
    }

    private void validateBookingRequest(BookingRequest req) {
        if (req.getMovieId() == null) {
            throw new IllegalArgumentException("movieId is required");
        }
        if (req.getShowDate() == null || req.getShowDate().isBlank()) {
            throw new IllegalArgumentException("showDate is required");
        }
        if (req.getShowTime() == null || req.getShowTime().isBlank()) {
            throw new IllegalArgumentException("showTime is required");
        }
        if (req.getSeats() == null || req.getSeats().isEmpty()) {
            throw new IllegalArgumentException("At least one seat is required");
        }
        if (req.getTotalPrice() == null) {
            throw new IllegalArgumentException("totalPrice is required");
        }
    }

    private Integer resolveSeatPrice(BookingRequest req, String seatId) {
        if (req.getSeatPrices() != null && req.getSeatPrices().get(seatId) != null) {
            return req.getSeatPrices().get(seatId);
        }
        return defaultPriceForSeat(seatId);
    }

    private Integer defaultPriceForSeat(String seatId) {
        String tier = getTier(seatId);
        return switch (tier) {
            case "economy" -> 150;
            case "standard" -> 300;
            default -> 500;
        };
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}