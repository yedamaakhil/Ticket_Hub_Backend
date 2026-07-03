package com.Springboot.Ticket_Booking_System.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.Springboot.Ticket_Booking_System.model.Booking;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${resend.from.email:onboarding@resend.dev}")
    private String fromEmail;

    @Value("${app.name:TicketHub}")
    private String appName;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Sends booking confirmation email via Resend's HTTP API.
     * Returns true if sent successfully.
     */
    public boolean sendBookingConfirmation(Booking booking, String toEmail) {
        if (booking == null) {
            log.warn("Email skipped: booking is null");
            return false;
        }
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Email skipped: recipient email is empty for booking {}", booking.getBookingRef());
            return false;
        }
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.error("Email skipped: RESEND_API_KEY is not set on the server");
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("from", appName + " <" + fromEmail + ">");
            body.put("to", new String[]{ toEmail.trim() });
            body.put("subject", "Booking Confirmed: " + getSafeString(booking.getMovieTitle(), "Movie"));
            body.put("html", buildEmailHtml(booking));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(RESEND_API_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                log.info("Confirmation email sent to {} for booking {} — Resend response: {}",
                    toEmail, booking.getBookingRef(), response.getBody());
                return true;
            } else {
                log.error("Resend returned non-success status {} for booking {}: {}",
                    response.getStatusCode(), booking.getBookingRef(), response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to send email to {} for booking {}: {}",
                toEmail, booking.getBookingRef(), e.getMessage(), e);
            return false;
        }
    }

    private String buildEmailHtml(Booking booking) {
        String movieTitle = getSafeString(booking.getMovieTitle(), "Movie");
        String bookingRef = getSafeString(booking.getBookingRef(), "BK-" + System.currentTimeMillis());
        String transactionId = getSafeString(booking.getTransactionId(), "TXN-" + System.currentTimeMillis());
        String showDate = formatDate(booking.getShowDate());
        String showTime = formatTime(booking.getShowTime());
        String theaterName = getSafeString(booking.getTheaterName(), "Cinema");
        String screenName = getSafeString(booking.getScreenName(), "1");
        String status = getSafeString(booking.getStatus(), "CONFIRMED");
        int totalPrice = booking.getTotalPrice() != null ? booking.getTotalPrice() : 0;
        String seatNumbers = (booking.getSeats() != null && !booking.getSeats().isEmpty())
            ? String.join(", ", booking.getSeats()) : "N/A";

        return "<!DOCTYPE html><html><head>"
            + "<style>"
            + "  body { background-color: #050505; margin: 0; padding: 40px 0; font-family: 'Segoe UI', Arial, sans-serif; }"
            + "  .ticket-wrapper { max-width: 900px; margin: 0 auto; background-color: #121212; border: 1px solid #333; border-radius: 16px; overflow: hidden; padding: 24px; color: #fff; }"
            + "  .app-name { color: #ff8080; font-size: 22px; font-weight: bold; margin-bottom: 8px; }"
            + "  .movie-title { font-size: 24px; font-weight: 800; margin: 0 0 8px 0; }"
            + "  .meta { color: #777; font-size: 13px; margin-bottom: 20px; }"
            + "  .label { color: #888; font-size: 11px; text-transform: uppercase; font-weight: bold; }"
            + "  .val { color: #eee; font-size: 16px; font-weight: 600; margin-bottom: 12px; }"
            + "  .seats { color: #ff4d4d; font-size: 18px; font-weight: 800; }"
            + "  .price { color: #00ff88; font-size: 22px; font-weight: 800; margin-top: 16px; }"
            + "  .ref { color: #666; font-size: 12px; font-family: monospace; margin-top: 16px; }"
            + "</style></head><body>"
            + "<div class='ticket-wrapper'>"
            + "  <div class='app-name'>TicketHub — Your E-Ticket</div>"
            + "  <h1 class='movie-title'>" + escapeHtml(movieTitle) + "</h1>"
            + "  <div class='meta'>" + buildGenreLine(booking) + " · " + status + "</div>"
            + "  <div class='label'>Date</div><div class='val'>" + showDate + "</div>"
            + "  <div class='label'>Time</div><div class='val'>" + showTime + "</div>"
            + "  <div class='label'>Theater</div><div class='val'>" + escapeHtml(theaterName) + " · Screen " + screenName + "</div>"
            + "  <div class='label'>Seats</div><div class='seats'>" + seatNumbers + "</div>"
            + "  <div class='price'>Total Paid: Rs." + formatCurrency(totalPrice) + "</div>"
            + "  <div class='ref'>Booking Ref: " + bookingRef + " · Txn: " + transactionId + "</div>"
            + "  <p style='color:#555;font-size:12px;margin-top:24px;'>Show this email at the cinema entrance. Enjoy your movie!</p>"
            + "</div></body></html>";
    }

    private String buildGenreLine(Booking booking) {
        String genre = booking.getMovieGenres() != null ? booking.getMovieGenres() : "Action, Drama";
        String lang = booking.getMovieLanguage() != null ? booking.getMovieLanguage() : "Telugu";
        Integer runtime = booking.getMovieRuntime() != null ? booking.getMovieRuntime() : 150;
        return genre + " · " + (runtime / 60) + "h " + (runtime % 60) + "m · " + lang.toUpperCase();
    }

    private String formatDate(String d) {
        try { return LocalDate.parse(d).format(DateTimeFormatter.ofPattern("dd MMM yyyy")); }
        catch (Exception e) { return d != null ? d : "N/A"; }
    }

    private String formatTime(String t) {
        if (t == null || t.isEmpty()) return "N/A";
        try {
            if (t.contains("AM") || t.contains("PM")) return t;
            String[] p = t.split(":");
            int h = Integer.parseInt(p[0]);
            return (h % 12 == 0 ? 12 : h % 12) + ":" + p[1] + (h >= 12 ? " PM" : " AM");
        } catch (Exception e) { return t; }
    }

    private String getSafeString(String v, String f) { return (v != null && !v.isEmpty()) ? v : f; }
    private String formatCurrency(int a) { return String.format("%,d", a); }
    private String escapeHtml(String t) {
        return t == null ? "" : t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}