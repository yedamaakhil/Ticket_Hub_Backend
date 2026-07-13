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
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${brevo.from.email:tickethub.online@gmail.com}")
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
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            log.error("Email skipped: BREVO_API_KEY is not set on the server");
            return false;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);
            headers.set("accept", "application/json");

            Map<String, String> sender = new HashMap<>();
            sender.put("name", appName);
            sender.put("email", fromEmail);

            Map<String, String> recipient = new HashMap<>();
            recipient.put("email", toEmail.trim());

            Map<String, Object> body = new HashMap<>();
            body.put("sender", sender);
            body.put("to", List.of(recipient));
            body.put("subject", "Booking Confirmed: " + getSafeString(booking.getMovieTitle(), "Movie"));
            body.put("htmlContent", buildEmailHtml(booking));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(BREVO_API_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                log.info("Confirmation email sent to {} for booking {} — Brevo: {}",
                        toEmail, booking.getBookingRef(), response.getBody());
                return true;
            } else {
                log.error("Brevo returned {} for booking {}: {}",
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
    //  EMAIL HTML — table-based, responsive
    //
    //  DESKTOP (≥601 px):  [Poster 130px bg-cover] | [Details] | [Barcode 150px]
    //  MOBILE  (≤600 px):  [Poster centered img] → [Details] → [Barcode centered]
    //
    //  Poster uses background-image on <td> so it fills 100% of card height.
    //  CSS duplicated in <head> + <body> so Gmail Web picks it up.
    // ─────────────────────────────────────────────────────────────────────────
    private String buildEmailHtml(Booking booking) {

        // ── Data extraction ────────────────────────────────────────────────
        String movieTitle    = getSafeString(booking.getMovieTitle(),    "Movie");
        String bookingRef    = getSafeString(booking.getBookingRef(),    "BK-" + System.currentTimeMillis());
        String transactionId = getSafeString(booking.getTransactionId(), "TXN-" + System.currentTimeMillis());
        String showDate      = formatDate(booking.getShowDate());
        String showTime      = formatTime(booking.getShowTime());
        String theaterName   = getSafeString(booking.getTheaterName(),   "Cinema");
        String screenName    = getSafeString(booking.getScreenName(),    "1");
        String status        = getSafeString(booking.getStatus(),        "CONFIRMED");
        int    totalPrice    = booking.getTotalPrice() != null ? booking.getTotalPrice() : 0;
        List<String> seats   = booking.getSeats();
        String seatsLine     = (seats != null && !seats.isEmpty()) ? String.join(", ", seats) : "N/A";
        String posterUrl     = resolvePosterUrl(booking.getMoviePosterPath());
        String barcodeUrl    = "https://barcodeapi.org/api/code128/" + urlEncode(bookingRef);
        String genreLine     = buildGenreLine(booking);

        // ── Shared CSS (duplicated in head + body for Gmail web) ──────────
        String css =
            "body,table,td,p,a,li,blockquote{-webkit-text-size-adjust:100%;-ms-text-size-adjust:100%;}" +
            "table,td{mso-table-lspace:0pt;mso-table-rspace:0pt;}" +
            "img{border:0;height:auto;line-height:100%;outline:none;text-decoration:none;}" +
            "@media only screen and (max-width:600px){" +
            "  .ew{width:100% !important;padding:0 10px !important;box-sizing:border-box !important;}" +
            "  .ticket{width:100% !important;}" +
            /* Hide desktop poster column on mobile */
            "  .col-poster{display:none !important;max-height:0 !important;overflow:hidden !important;}" +
            /* Stack details + barcode vertically */
            "  .col-details{display:block !important;width:100% !important;padding:18px 16px !important;box-sizing:border-box !important;}" +
            "  .col-barcode{display:block !important;width:100% !important;text-align:center !important;" +
            "    border-left:none !important;border-top:1px solid #2a2a2a !important;padding:20px 16px !important;}" +
            /* Show mobile poster */
            "  .mob-poster{display:block !important;text-align:center !important;margin-bottom:14px !important;}" +
            /* Info grids */
            "  .ig td{display:block !important;width:100% !important;padding-bottom:8px !important;text-align:center !important;}" +
            "  .ig tr{display:block !important;}" +
            "  .sp td{display:block !important;width:100% !important;text-align:center !important;padding-bottom:8px !important;}" +
            "  .sp tr{display:block !important;}" +
            "  .tb td{display:block !important;width:100% !important;text-align:center !important;}" +
            "  .tb tr{display:block !important;}" +
            /* Typography */
            "  .movie-title{font-size:17px !important;}" +
            "  .genre-line{font-size:10px !important;}" +
            "  .badge{display:inline-block !important;margin:6px auto 0 !important;}" +
            "  .lbl{font-size:9px !important;}" +
            "  .val{font-size:13px !important;}" +
            "  .seats-val{font-size:14px !important;}" +
            "  .price-val{font-size:17px !important;}" +
            "  .ref-line{text-align:center !important;}" +
            "  .barcode-box{display:inline-block !important;}" +
            "  .txn-id{font-size:8px !important;}" +
            "  .footer-title{font-size:13px !important;}" +
            "}";

        return "<!DOCTYPE html>" +
            "<html lang='en'><head><meta charset='utf-8'>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "<meta http-equiv='X-UA-Compatible' content='IE=edge'>" +
            "<title>Booking Confirmed</title>" +
            "<style>" + css + "</style>" +
            "</head>" +

            "<body style='margin:0;padding:32px 0;background-color:#0a0a0a;" +
            "font-family:\"Segoe UI\",Arial,Helvetica,sans-serif;'>" +
            "<style>" + css + "</style>" +

            /* ── Outer wrapper ─────────────────────────────────────────── */
            "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0'>" +
            "<tr><td align='center' style='padding:0 12px;'>" +
            "<table role='presentation' width='640' cellpadding='0' cellspacing='0' border='0'" +
            " class='ew' style='max-width:640px;width:640px;'>" +

            /* ── App header ────────────────────────────────────────────── */
            "<tr><td align='center' style='padding-bottom:16px;'>" +
            "<div style='color:#ff8080;font-size:20px;font-weight:800;letter-spacing:0.5px;'>" +
                escapeHtml(appName) + "</div>" +
            "<div style='color:#4ade80;font-size:11px;font-weight:700;margin-top:4px;letter-spacing:0.5px;'>" +
                "PAYMENT SUCCESSFUL &nbsp;&middot;&nbsp; BOOKING CONFIRMED</div>" +
            "</td></tr>" +

            /* ── Ticket card ───────────────────────────────────────────── */
            "<tr><td>" +
            "<table role='presentation' class='ticket' width='640' cellpadding='0' cellspacing='0' border='0'" +
            " style='max-width:640px;width:640px;border-radius:16px;border:1px solid #2a2a2a;overflow:hidden;'>" +
            "<tr>" +

            /* ── COL 1: Poster as background-image — fills full card height ── */
            // background-image on <td> stretches automatically to match sibling height.
            // object-fit:cover equivalent via background-size:cover + background-position:center top.
            "<td class='col-poster' width='130'" +
            " style='width:130px;min-width:130px;padding:0;" +
            "background-image:url(\"" + posterUrl + "\");" +
            "background-size:cover;" +
            "background-position:center top;" +
            "background-color:#2a2a2a;'>" +
            "</td>" +

            /* ── COL 2: Booking details ────────────────────────────────── */
            "<td class='col-details' valign='top'" +
            " style='background-color:#161616;padding:18px 20px;vertical-align:top;'>" +

            /* Mobile poster — hidden on desktop, visible on mobile */
            "<div class='mob-poster' style='display:none;'>" +
            "<img src='" + posterUrl + "' width='90' alt='" + escapeHtml(movieTitle) + "'" +
            " style='width:90px;height:auto;border-radius:10px;display:inline-block;' /></div>" +

            /* Title + status badge */
            "<table role='presentation' class='tb' width='100%' cellpadding='0' cellspacing='0' border='0'><tr>" +
            "<td valign='top' style='padding-right:10px;'>" +
            "<div class='movie-title' style='color:#ffffff;font-size:16px;font-weight:800;line-height:1.3;'>" +
                escapeHtml(movieTitle) + "</div>" +
            "<div class='genre-line' style='color:#888888;font-size:10px;margin-top:4px;'>" +
                genreLine + "</div>" +
            "</td>" +
            "<td valign='top' align='right' style='white-space:nowrap;'>" +
            "<span class='badge' style='display:inline-block;background:rgba(74,222,128,0.15);" +
                "color:#4ade80;font-size:9px;font-weight:700;padding:4px 10px;" +
                "border-radius:999px;white-space:nowrap;'>" + escapeHtml(status) + "</span>" +
            "</td>" +
            "</tr></table>" +

            "<div style='border-top:1px solid #2a2a2a;margin:12px 0 10px;'></div>" +

            /* Date + Time */
            "<table role='presentation' class='ig' width='100%' cellpadding='0' cellspacing='0' border='0'><tr>" +
            "<td width='50%' valign='top' style='padding-bottom:0;'>" +
            "<div class='lbl' style='color:#777777;font-size:9px;text-transform:uppercase;" +
                "font-weight:700;letter-spacing:0.5px;margin-bottom:3px;'>Date</div>" +
            "<div class='val' style='color:#ffffff;font-size:13px;font-weight:700;'>" + showDate + "</div>" +
            "</td>" +
            "<td width='50%' valign='top'>" +
            "<div class='lbl' style='color:#777777;font-size:9px;text-transform:uppercase;" +
                "font-weight:700;letter-spacing:0.5px;margin-bottom:3px;'>Time</div>" +
            "<div class='val' style='color:#ffffff;font-size:13px;font-weight:700;'>" + showTime + "</div>" +
            "</td>" +
            "</tr></table>" +

            "<div style='height:10px;'></div>" +

            /* Theater + Screen */
            "<table role='presentation' class='ig' width='100%' cellpadding='0' cellspacing='0' border='0'><tr>" +
            "<td width='50%' valign='top' style='padding-bottom:0;'>" +
            "<div class='lbl' style='color:#777777;font-size:9px;text-transform:uppercase;" +
                "font-weight:700;letter-spacing:0.5px;margin-bottom:3px;'>Theater</div>" +
            "<div class='val' style='color:#ffffff;font-size:13px;font-weight:700;'>" +
                escapeHtml(theaterName) + "</div>" +
            "</td>" +
            "<td width='50%' valign='top'>" +
            "<div class='lbl' style='color:#777777;font-size:9px;text-transform:uppercase;" +
                "font-weight:700;letter-spacing:0.5px;margin-bottom:3px;'>Screen</div>" +
            "<div class='val' style='color:#ffffff;font-size:13px;font-weight:700;'>Screen " +
                escapeHtml(screenName) + "</div>" +
            "</td>" +
            "</tr></table>" +

            "<div style='border-top:1px solid #2a2a2a;margin:12px 0 10px;'></div>" +

            /* Seats + Price */
            "<table role='presentation' class='sp' width='100%' cellpadding='0' cellspacing='0' border='0'><tr>" +
            "<td width='50%' valign='top' style='padding-bottom:0;'>" +
            "<div class='lbl' style='color:#777777;font-size:9px;text-transform:uppercase;" +
                "font-weight:700;letter-spacing:0.5px;margin-bottom:3px;'>Seats</div>" +
            "<div class='seats-val' style='color:#ff5a5a;font-size:15px;font-weight:800;'>" +
                escapeHtml(seatsLine) + "</div>" +
            "</td>" +
            "<td width='50%' valign='top' align='right'>" +
            "<div class='lbl' style='color:#777777;font-size:9px;text-transform:uppercase;" +
                "font-weight:700;letter-spacing:0.5px;margin-bottom:3px;'>Total Paid</div>" +
            "<div class='price-val' style='color:#ffffff;font-size:18px;font-weight:800;'>" +
                "&#8377;" + formatCurrency(totalPrice) + "</div>" +
            "</td>" +
            "</tr></table>" +

            "<div class='ref-line' style='color:#555555;font-size:9px;margin-top:10px;" +
                "font-family:monospace;'>Ref: " + bookingRef + "</div>" +

            "</td>" + /* end col-details */

            /* ── COL 3: Barcode panel ──────────────────────────────────── */
            "<td class='col-barcode' valign='middle' align='center' width='150'" +
            " style='width:150px;background-color:#1c1c1c;padding:20px 12px;" +
            "border-left:1px solid #2a2a2a;vertical-align:middle;text-align:center;'>" +

            "<div class='barcode-box' style='display:inline-block;background:#ffffff;" +
                "border-radius:10px;padding:12px 10px;'>" +
            "<img src='" + barcodeUrl + "' width='110' alt='" + escapeHtml(bookingRef) + "'" +
            " style='display:block;width:110px;height:auto;' />" +
            "<div style='color:#111111;font-size:9px;font-weight:700;font-family:monospace;" +
                "letter-spacing:1px;margin-top:7px;text-align:center;'>" + escapeHtml(bookingRef) + "</div>" +
            "</div>" +

            "<div class='txn-id' style='color:#666666;font-size:8px;font-family:monospace;" +
                "margin-top:10px;word-break:break-all;text-align:center;line-height:1.4;'>" +
                escapeHtml(transactionId) + "</div>" +

            "</td>" + /* end col-barcode */

            "</tr></table>" + /* end ticket card */
            "</td></tr>" +

            /* ── Footer ────────────────────────────────────────────────── */
            "<tr><td align='center' style='padding-top:20px;'>" +
            "<div class='footer-title' style='color:#ffffff;font-size:14px;font-weight:700;'>Enjoy your show!</div>" +
            "<div style='color:#555555;font-size:10px;margin-top:4px;'>" +
                "Please arrive 15 minutes before showtime</div>" +
            "</td></tr>" +

            "</table>" + /* end ew */
            "</td></tr></table>" + /* end outer wrapper */
            "</body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private String buildGenreLine(Booking booking) {
        String genre    = booking.getMovieGenres()  != null ? booking.getMovieGenres()  : "Action, Drama";
        String lang     = booking.getMovieLanguage() != null ? booking.getMovieLanguage() : "Telugu";
        Integer runtime = booking.getMovieRuntime() != null ? booking.getMovieRuntime() : 150;
        return escapeHtml(genre) + " &middot; " +
               (runtime / 60) + "h " + (runtime % 60) + "m &middot; " +
               escapeHtml(lang.toUpperCase());
    }

    private String resolvePosterUrl(String posterPath) {
        if (posterPath == null || posterPath.isBlank()) {
            return "https://via.placeholder.com/130x190/1a1a1a/555555?text=No+Image";
        }
        if (posterPath.startsWith("http")) return posterPath;
        return "https://image.tmdb.org/t/p/w342" + posterPath;
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

    private String getSafeString(String v, String fallback) {
        return (v != null && !v.isEmpty()) ? v : fallback;
    }

    private String formatCurrency(int amount) {
        return String.format("%,d", amount);
    }

    private String escapeHtml(String t) {
        return t == null ? "" : t
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}