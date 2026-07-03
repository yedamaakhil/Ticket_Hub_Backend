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

    // ─────────────────────────────────────────────────────────────────────────
    //  HTML TICKET — compact table-based layout, matches My Shows ticket card.
    //  Poster column uses an inner height:100% table so the poster fills the
    //  full card height instead of stopping short.
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

        String seatsLine = (seats != null && !seats.isEmpty())
            ? String.join(", ", seats)
            : "N/A";

        return "<!DOCTYPE html><html><head><meta charset='utf-8'>"
            + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
            + "</head>"
            + "<body style=\"margin:0;padding:32px 12px;background-color:#0a0a0a;font-family:'Segoe UI',Arial,sans-serif;\">"
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='max-width:680px;margin:0 auto;'>"
            + "<tr><td>"

            + "<div style='text-align:center;margin-bottom:14px;'>"
            + "  <span style='color:#ff8080;font-size:19px;font-weight:800;letter-spacing:0.5px;'>" + escapeHtml(appName) + "</span>"
            + "  <div style='color:#4ade80;font-size:11px;font-weight:600;margin-top:3px;'>PAYMENT SUCCESSFUL &middot; BOOKING CONFIRMED</div>"
            + "</div>"

            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' "
            + "style='border-radius:16px;border:1px solid #262626;overflow:hidden;'>"
            + "<tr>"

            // ── LEFT: details panel ──────────────────────────────────────────
            + "<td valign='top' width='72%' style='background-color:#161616;padding:0;'>"
            + "<table role='presentation' width='100%' height='100%' cellpadding='0' cellspacing='0'><tr>"

            // poster — inner table height:100% makes this cell stretch full column height
            + "<td width='110' valign='top' style='padding:0;background-color:#2a2a2a;'>"
            + "<img src='" + posterUrl + "' width='110' alt='" + escapeHtml(movieTitle) + "' "
            + "style='width:110px;height:100%;min-height:170px;object-fit:cover;display:block;' />"
            + "</td>"

            // details
            + "<td valign='top' style='padding:14px 18px;'>"

            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0'><tr>"
            + "<td valign='top'>"
            + "<div style='color:#ffffff;font-size:16px;font-weight:800;margin-bottom:4px;'>" + escapeHtml(movieTitle) + "</div>"
            + "<div style='color:#999999;font-size:10px;'>" + buildGenreLine(booking) + "</div>"
            + "</td>"
            + "<td valign='top' align='right'>"
            + "<span style='display:inline-block;background:rgba(74,222,128,0.15);color:#4ade80;"
            + "font-size:9px;font-weight:700;padding:4px 10px;border-radius:999px;white-space:nowrap;'>" + escapeHtml(status) + "</span>"
            + "</td>"
            + "</tr></table>"

            + "<div style='height:12px;'></div>"

            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0'><tr>"
            + "<td width='50%' valign='top'>"
            + "<div style='color:#888888;font-size:9px;text-transform:uppercase;font-weight:700;letter-spacing:0.5px;margin-bottom:3px;'>Date</div>"
            + "<div style='color:#ffffff;font-size:13px;font-weight:700;'>" + showDate + "</div>"
            + "</td>"
            + "<td width='50%' valign='top'>"
            + "<div style='color:#888888;font-size:9px;text-transform:uppercase;font-weight:700;letter-spacing:0.5px;margin-bottom:3px;'>Time</div>"
            + "<div style='color:#ffffff;font-size:13px;font-weight:700;'>" + showTime + "</div>"
            + "</td>"
            + "</tr></table>"

            + "<div style='height:10px;'></div>"

            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0'><tr>"
            + "<td width='50%' valign='top'>"
            + "<div style='color:#888888;font-size:9px;text-transform:uppercase;font-weight:700;letter-spacing:0.5px;margin-bottom:3px;'>Theater</div>"
            + "<div style='color:#ffffff;font-size:13px;font-weight:700;'>" + escapeHtml(theaterName) + "</div>"
            + "</td>"
            + "<td width='50%' valign='top'>"
            + "<div style='color:#888888;font-size:9px;text-transform:uppercase;font-weight:700;letter-spacing:0.5px;margin-bottom:3px;'>Screen</div>"
            + "<div style='color:#ffffff;font-size:13px;font-weight:700;'>Screen " + escapeHtml(screenName) + "</div>"
            + "</td>"
            + "</tr></table>"

            + "<div style='height:10px;'></div>"
            + "<div style='border-top:1px solid #262626;'></div>"
            + "<div style='height:10px;'></div>"

            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0'><tr>"
            + "<td width='50%' valign='top'>"
            + "<div style='color:#888888;font-size:9px;text-transform:uppercase;font-weight:700;letter-spacing:0.5px;margin-bottom:3px;'>Seats</div>"
            + "<div style='color:#ff5a5a;font-size:14px;font-weight:800;'>" + escapeHtml(seatsLine) + "</div>"
            + "</td>"
            + "<td width='50%' valign='top' align='right'>"
            + "<div style='color:#888888;font-size:9px;text-transform:uppercase;font-weight:700;letter-spacing:0.5px;margin-bottom:3px;'>Total paid</div>"
            + "<div style='color:#ffffff;font-size:17px;font-weight:800;'>&#8377;" + formatCurrency(totalPrice) + "</div>"
            + "</td>"
            + "</tr></table>"

            + "<div style='height:8px;'></div>"
            + "<div style='color:#666666;font-size:9px;'>Ref: " + bookingRef + "</div>"

            + "</td>"
            + "</tr></table>"
            + "</td>"

            // ── RIGHT: barcode panel ─────────────────────────────────────────
            + "<td valign='middle' align='center' width='28%' style='background-color:#1c1c1c;padding:16px 12px;border-left:1px solid #262626;'>"
            + "<div style='background:#ffffff;display:inline-block;padding:12px 10px;border-radius:10px;'>"
            + "<img src='" + barcodeUrl + "' alt='" + bookingRef + "' width='130' style='display:block;max-width:130px;' />"
            + "<div style='color:#111111;font-size:9px;letter-spacing:1px;font-family:monospace;margin-top:6px;text-align:center;'>" + bookingRef + "</div>"
            + "</div>"
            + "<div style='color:#777777;font-size:8px;font-family:monospace;margin-top:10px;word-break:break-all;text-align:center;'>" + transactionId + "</div>"
            + "</td>"

            + "</tr></table>"

            + "<div style='text-align:center;margin-top:16px;'>"
            + "<div style='color:#ffffff;font-size:14px;font-weight:700;'>Enjoy your show!</div>"
            + "<div style='color:#666666;font-size:10px;margin-top:3px;'>Please arrive 15 minutes before showtime</div>"
            + "</div>"

            + "</td></tr></table>"
            + "</body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private String buildGenreLine(Booking booking) {
        String genre = booking.getMovieGenres() != null ? booking.getMovieGenres() : "Action, Drama";
        String lang = booking.getMovieLanguage() != null ? booking.getMovieLanguage() : "Telugu";
        Integer runtime = booking.getMovieRuntime() != null ? booking.getMovieRuntime() : 150;
        return escapeHtml(genre) + " &middot; " + (runtime / 60) + "h " + (runtime % 60) + "m &middot; " + lang.toUpperCase();
    }

    private String resolvePosterUrl(String posterPath) {
        if (posterPath == null || posterPath.isBlank()) {
            return "https://via.placeholder.com/110x220/1a1a1a/555555?text=No+Image";
        }
        if (posterPath.startsWith("http")) {
            return posterPath;
        }
        return "https://image.tmdb.org/t/p/w200" + posterPath;
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
    private String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}