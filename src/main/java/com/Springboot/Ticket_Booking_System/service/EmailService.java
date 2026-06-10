package com.Springboot.Ticket_Booking_System.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.Springboot.Ticket_Booking_System.model.Booking;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendBookingConfirmation(Booking booking, String toEmail) {
        if (toEmail == null || toEmail.trim().isEmpty() || booking == null) return;
        if (fromEmail == null || fromEmail.isBlank()) {
            System.err.println("Email skipped: SPRING_MAIL_USERNAME is not configured");
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Booking Confirmed: " + getSafeString(booking.getMovieTitle(), "Movie"));
            helper.setText(buildEmailHtml(booking), true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Email Error: " + e.getMessage());
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
        String seatNumbers = (booking.getSeats() != null) ? String.join(", ", booking.getSeats()) : "N/A";

        String posterPath = booking.getMoviePosterPath();
        String posterUrl = (posterPath != null && !posterPath.isEmpty())
            ? (posterPath.startsWith("http") ? posterPath : "https://image.tmdb.org/t/p/w500" + posterPath)
            : "https://via.placeholder.com/300x450?text=No+Poster";

        return "<!DOCTYPE html><html><head>"
            + "<style>"
            + "  body { background-color: #050505; margin: 0; padding: 40px 0; font-family: 'Segoe UI', Arial, sans-serif; }"
            + "  /* Increased width to 900px to fix congestion */"
            + "  .ticket-wrapper { width: 1000px; height: 350px; margin: 0 auto; background-color: #121212; border: 1px solid #333; border-radius: 16px; overflow: hidden; display: table; border-collapse: separate; }"
            + "  .poster-cell { display: table-cell; width: 230pxs; vertical-align: top; background-color: #000; }"
            + "  .poster-img { width: 230px; height: 360px; display: block; object-fit: cover; }"
            + "  /* Added more horizontal padding (40px) */"
            + "  .info-cell { display: table-cell; vertical-align: top; padding: 30px 40px; color: #ffffff; }"
            + "  .badge { float: right; color: #00ff88; border: 1px solid #00ff88; padding: 5px 15px; border-radius: 20px; font-size: 10px; font-weight: bold; text-transform: uppercase; background: rgba(0,255,136,0.1); letter-spacing: 0.5px; }"
            + "  .app-name { color: #ff8080; font-size: 25px; font-weight: bold; letter-spacing: 1px; margin-bottom: 8px; }"
            + "  .movie-title { font-size: 26px; font-weight: 800; margin: 0; letter-spacing: -0.5px; text-transform: uppercase; max-width: 80%; }"
            + "  .meta { color: #777; font-size: 13px; margin: 8px 0 25px 0; }"
            + "  .label { color: #555; font-size: 11px; text-transform: uppercase; font-weight: bold; letter-spacing: 1px; padding-bottom: 4px; }"
            + "  .val { color: #eee; font-size: 18px; font-weight: 600; padding-bottom: 20px; }"
            + "  .seats-highlight { color: #ff4d4d; font-size: 20px; font-weight: 800; }"
            + "  .divider { display: table-cell; width: 2px; border-left: 2px dotted #333; background-color: #121212; }"
            + "  .stub-cell { display: table-cell; width: 220px; background-color: #121212; vertical-align: middle; text-align: center; padding: 20px; }"
            + "  .barcode-card { background: #ffffff; padding: 15px; border-radius: 12px; display: inline-block; box-shadow: 0 4px 15px rgba(0,0,0,0.5); }"
            + "  .price-text { color: #ffffff; font-size: 28px; font-weight: 800; margin-top: 15px; }"
            + "  .ref-text { color: #444; font-size: 10px; font-family: monospace; margin-top: 10px; }"
            + "  .barcode-img { width: 200px; height: 70px; margin: 5px 0; }"
            + "</style></head><body>"
            + "<div class='ticket-wrapper'>"
            + "  <div class='poster-cell'><img src='" + posterUrl + "' class='poster-img'></div>"
            + "  <div class='info-cell'>"
            + "    <div class='badge'>" + status + "</div>"
            + "    <div class='app-name'>🎬 TicketHub</div>"
            + "    <h1 class='movie-title'>" + escapeHtml(movieTitle) + "</h1>"
            + "    <div class='meta'>" + buildGenreLine(booking) + "</div>"
            + "    <table width='100%' border='0' cellspacing='0' cellpadding='0'>"
            + "      <tr>"
            + "        <td width='50%'><div class='label'>Date</div><div class='val'>" + showDate + "</div></td>"
            + "        <td><div class='label'>Time</div><div class='val'>" + showTime + "</div></td>"
            + "      </tr>"
            + "      <tr>"
            + "        <td><div class='label'>Theater</div><div class='val'>" + escapeHtml(theaterName) + "</div></td>"
            + "        <td><div class='label'>Screen</div><div class='val'>" + screenName + "</div></td>"
            + "      </tr>"
            + "    </table>"
            + "    <div class='label' style='margin-top:10px;'>Seats</div>"
            + "    <div class='seats-highlight'>" + seatNumbers + "</div>"
            + "  </div>"
            + "  <div class='divider'></div>"
            + "  <div class='stub-cell'>"
            + "    <div class='barcode-card'>"
            + "      <img src='https://barcode.tec-it.com/barcode.ashx?data=" + bookingRef + "&code=Code128&dpi=96&dataseparator=' alt='Barcode' class='barcode-img' />"
            + "      <div class='ref-text'>" + transactionId + "</div>"
            + "    </div>"
            + "    <div class='label' style='margin-top:20px;'>Total Paid</div>"
            + "    <div class='price-text'>₹" + formatCurrency(totalPrice) + "</div>"
            + "  </div>"
            + "</div>"
            + "</body></html>";
    }

    private String generateBarcodeSvg(String text) {
        int width = 140; int height = 60;
        StringBuilder svg = new StringBuilder();
        svg.append("<svg width='").append(width).append("' height='").append(height)
           .append("' viewBox='0 0 ").append(width).append(" ").append(height).append("' xmlns='http://www.w3.org/2000/svg'>");
        svg.append("<rect width='100%' height='100%' fill='#ffffff'/>");
        long seed = Math.abs(text.hashCode());
        int x = 10;
        while (x < width - 15) {
            int bw = (seed % 3 == 0) ? 3 : 1;
            seed = (seed * 13 + 7) % 100000;
            svg.append("<rect x='").append(x).append("' y='5' width='").append(bw).append("' height='40' fill='#000000'/>");
            x += bw + 1;
        }
        svg.append("<text x='70' y='55' text-anchor='middle' font-family='monospace' font-size='8' fill='#000'>").append(text).append("</text>");
        svg.append("</svg>");
        return svg.toString();
    }

    private String buildGenreLine(Booking booking) {
        String genre = booking.getMovieGenres() != null ? booking.getMovieGenres() : "Action, Drama";
        String lang = booking.getMovieLanguage() != null ? booking.getMovieLanguage() : "TELUGU";
        Integer runtime = booking.getMovieRuntime() != null ? booking.getMovieRuntime() : 150;
        return genre + " · " + (runtime / 60) + "h " + (runtime % 60) + "m · " + lang.toUpperCase();
    }

    private String formatDate(String d) {
        try { return LocalDate.parse(d).format(DateTimeFormatter.ofPattern("dd MMM yyyy")); } 
        catch (Exception e) { return d; }
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
    private String escapeHtml(String t) { return t == null ? "" : t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
}