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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
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

    // Same tier boundaries used in BookingService / frontend
    private static final List<Character> PREMIUM_ROWS_START = List.of('K');

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
            body.put("subject", "🎟️ Booking Confirmed: " + getSafeString(booking.getMovieTitle(), "Movie"));
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

    // ─────────────────────────────────────────────────────────────────────────
    //  HTML TICKET — table-based layout for email-client compatibility,
    //  visually mirrors the "My Shows" / confirmation ticket card.
    // ─────────────────────────────────────────────────────────────────────────
    private String buildEmailHtml(Booking booking) {
        String movieTitle    = getSafeString(booking.getMovieTitle(), "Movie");
        String bookingRef    = getSafeString(booking.getBookingRef(), "BK-" + System.currentTimeMillis());
        String transactionId = getSafeString(booking.getTransactionId(), "TXN-" + System.currentTimeMillis());
        String showDate      = formatDate(booking.getShowDate());
        String showTime      = formatTime(booking.getShowTime());
        String theaterName   = getSafeString(booking.getTheaterName(), "Cinema");
        String screenName    = getSafeString(booking.getScreenName(), "1");
        String status        = getSafeString(booking.getStatus(), "CONFIRMED");
        int totalPrice        = booking.getTotalPrice() != null ? booking.getTotalPrice() : 0;
        List<String> seats     = booking.getSeats();

        String posterUrl = resolvePosterUrl(booking.getMoviePosterPath());
        String barcodeUrl = "https://barcodeapi.org/api/code128/" + urlEncode(bookingRef);

        StringBuilder seatBadges = new StringBuilder();
        if (seats != null && !seats.isEmpty()) {
            for (String seat : seats) {
                String[] colors = tierColors(getSeatTier(seat));
                seatBadges.append(
                    "<span style=\"display:inline-block;margin:2px 4px 2px 0;padding:5px 12px;"
                    + "border-radius:999px;font-size:12px;font-weight:700;"
                    + "background:" + colors[1] + ";color:" + colors[0] + ";"
                    + "border:1px solid " + colors[0] + ";\">" + escapeHtml(seat) + "</span>"
                );
            }
        } else {
            seatBadges.append("N/A");
        }

        return "<!DOCTYPE html><html><head><meta charset='utf-8'>"
            + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
            + "</head>"
            + "<body style=\"margin:0;padding:32px 12px;background-color:#050505;font-family:'Segoe UI',Arial,sans-serif;\">"
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='max-width:600px;margin:0 auto;'>"
            + "<tr><td>"

            // Header brand line
            + "<div style='text-align:center;margin-bottom:16px;'>"
            + "  <span style='color:#ff8080;font-size:20px;font-weight:800;letter-spacing:0.5px;'>🎟️ " + escapeHtml(appName) + "</span>"
            + "  <div style='color:#4ade80;font-size:12px;font-weight:600;margin-top:4px;'>PAYMENT SUCCESSFUL · BOOKING CONFIRMED</div>"
            + "</div>"

            // Ticket card
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' "
            + "style='background-color:#121212;border:1px solid #2a2a2a;border-radius:16px;overflow:hidden;'>"

            // Movie row: poster + info
            + "<tr><td style='padding:20px;border-bottom:1px solid #2a2a2a;'>"
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0'><tr>"
            + "<td width='96' valign='top' style='padding-right:16px;'>"
            + "<img src='" + posterUrl + "' width='96' height='128' alt='" + escapeHtml(movieTitle) + "' "
            + "style='width:96px;height:128px;object-fit:cover;border-radius:10px;display:block;background:#1a1a1a;' />"
            + "</td>"
            + "<td valign='middle'>"
            + "<div style='color:#ffffff;font-size:19px;font-weight:800;margin-bottom:6px;'>" + escapeHtml(movieTitle) + "</div>"
            + "<div style='color:#888888;font-size:12px;margin-bottom:4px;'>" + buildGenreLine(booking) + "</div>"
            + "<div style='color:#facc15;font-size:11px;font-weight:700;background:rgba(255,255,255,0.08);"
            + "display:inline-block;padding:2px 8px;border-radius:999px;'>" + escapeHtml(status) + "</div>"
            + "</td></tr></table>"
            + "</td></tr>"

            // Date / Time row
            + "<tr><td style='padding:18px 20px;border-bottom:1px solid #2a2a2a;'>"
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0'><tr>"
            + "<td width='50%'>"
            + "<div style='color:#888888;font-size:10px;text-transform:uppercase;font-weight:700;letter-spacing:0.5px;margin-bottom:4px;'>📅 Date</div>"
            + "<div style='color:#eeeeee;font-size:14px;font-weight:600;'>" + showDate + "</div>"
            + "</td>"
            + "<td width='50%'>"
            + "<div style='color:#888888;font-size:10px;text-transform:uppercase;font-weight:700;letter-spacing:0.5px;margin-bottom:4px;'>🕐 Time</div>"
            + "<div style='color:#eeeeee;font-size:14px;font-weight:600;'>" + showTime + "</div>"
            + "</td></tr></table>"
            + "</td></tr>"

            // Theater row
            + "<tr><td style='padding:18px 20px;border-bottom:1px solid #2a2a2a;'>"
            + "<div style='color:#888888;font-size:10px;text-transform:uppercase;font-weight:700;letter-spacing:0.5px;margin-bottom:4px;'>🎬 Theater &amp; Screen</div>"
            + "<div style='color:#eeeeee;font-size:14px;font-weight:600;'>" + escapeHtml(theaterName) + " · Screen " + escapeHtml(screenName) + "</div>"
            + "</td></tr>"

            // Seats row
            + "<tr><td style='padding:18px 20px;border-bottom:1px solid #2a2a2a;'>"
            + "<div style='color:#888888;font-size:10px;text-transform:uppercase;font-weight:700;letter-spacing:0.5px;margin-bottom:8px;'>🎫 Seats</div>"
            + "<div>" + seatBadges + "</div>"
            + "</td></tr>"

            // Total paid row
            + "<tr><td style='padding:18px 20px;'>"
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0'><tr>"
            + "<td style='color:#888888;font-size:13px;'>Total Paid</td>"
            + "<td align='right' style='color:#00ff88;font-size:22px;font-weight:800;'>₹" + formatCurrency(totalPrice) + "</td>"
            + "</tr></table>"
            + "</td></tr>"

            // Dashed tear line
            + "<tr><td style='padding:0 20px;'>"
            + "<div style='border-top:2px dashed #333333;'></div>"
            + "</td></tr>"

            // Barcode
            + "<tr><td style='padding:24px 20px;text-align:center;'>"
            + "<div style='background:#ffffff;display:inline-block;padding:10px 16px;border-radius:10px;'>"
            + "<img src='" + barcodeUrl + "' alt='" + bookingRef + "' height='55' style='display:block;' />"
            + "</div>"
            + "<div style='color:#888888;font-size:12px;letter-spacing:2px;font-family:monospace;margin-top:10px;'>" + bookingRef + "</div>"
            + "</td></tr>"

            // Footer
            + "<tr><td style='padding:0 20px 24px;text-align:center;'>"
            + "<div style='color:#ffffff;font-size:15px;font-weight:700;'>Enjoy your show!</div>"
            + "<div style='color:#666666;font-size:11px;margin-top:4px;'>Please arrive 15 minutes before showtime</div>"
            + "</td></tr>"

            + "</table>" // end ticket card

            // Txn ref below card
            + "<div style='text-align:center;color:#555555;font-size:11px;font-family:monospace;margin-top:16px;'>"
            + "Txn: " + transactionId
            + "</div>"

            + "<div style='text-align:center;color:#444444;font-size:11px;margin-top:20px;'>"
            + "Show this email at the cinema entrance."
            + "</div>"

            + "</td></tr></table>"
            + "</body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /** Same tier boundaries as BookingService.getTier() / frontend SEAT_PRICING */
    private String getSeatTier(String seatId) {
        if (seatId == null || seatId.isEmpty()) return "economy";
        char row = Character.toUpperCase(seatId.charAt(0));
        if (row <= 'B') return "economy";
        if (row <= 'J') return "standard";
        return "premium";
    }

    /** [borderColor, backgroundColor] — mirrors TIER_COLORS in the frontend */
    private String[] tierColors(String tier) {
        return switch (tier) {
            case "premium"  -> new String[]{ "#4ade80", "rgba(74,222,128,0.10)" };  // green
            case "standard" -> new String[]{ "#facc15", "rgba(250,204,21,0.10)" };  // yellow
            default         -> new String[]{ "#ff8080", "rgba(255,128,128,0.10)" }; // primary/red
        };
    }

    /** Same fallback logic as MyShows.jsx poster rendering */
    private String resolvePosterUrl(String posterPath) {
        if (posterPath == null || posterPath.isBlank()) {
            return "https://via.placeholder.com/96x128/1a1a1a/555555?text=No+Image";
        }
        if (posterPath.startsWith("http")) {
            return posterPath;
        }
        return "https://image.tmdb.org/t/p/w200" + posterPath;
    }

    private String buildGenreLine(Booking booking) {
        String genre = booking.getMovieGenres() != null ? booking.getMovieGenres() : "Action, Drama";
        String lang = booking.getMovieLanguage() != null ? booking.getMovieLanguage() : "Telugu";
        Integer runtime = booking.getMovieRuntime() != null ? booking.getMovieRuntime() : 150;
        return escapeHtml(genre) + " · " + (runtime / 60) + "h " + (runtime % 60) + "m · " + lang.toUpperCase();
    }

    private String formatDate(String d) {
        try { return LocalDate.parse(d).format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")); }
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
    private String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}