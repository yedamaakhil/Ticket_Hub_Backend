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
			body.put("to", new String[] { toEmail.trim() });
			body.put("subject", "🎟️ Booking Confirmed: " + getSafeString(booking.getMovieTitle(), "Movie"));
			body.put("html", buildEmailHtml(booking));

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
			ResponseEntity<String> response = restTemplate.postForEntity(RESEND_API_URL, request, String.class);

			if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
				log.info("Confirmation email sent to {} for booking {} — Resend response: {}", toEmail,
						booking.getBookingRef(), response.getBody());
				return true;
			} else {
				log.error("Resend returned non-success status {} for booking {}: {}", response.getStatusCode(),
						booking.getBookingRef(), response.getBody());
				return false;
			}
		} catch (Exception e) {
			log.error("Failed to send email to {} for booking {}: {}", toEmail, booking.getBookingRef(), e.getMessage(),
					e);
			return false;
		}
	}

	// ─────────────────────────────────────────────────────────────────────────
	// HTML TICKET — table-based layout for email-client compatibility,
	// visually mirrors the "My Shows" / confirmation ticket card.
	// ─────────────────────────────────────────────────────────────────────────
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
		List<String> seats = booking.getSeats();

		String posterUrl = resolvePosterUrl(booking.getMoviePosterPath());
		String barcodeUrl = "https://barcodeapi.org/api/code128/" + urlEncode(bookingRef);

		StringBuilder seatBadges = new StringBuilder();
		if (seats != null && !seats.isEmpty()) {
			for (String seat : seats) {
				seatBadges.append(
						"<span style=\"display:inline-block;margin:0 6px 0 0;color:#ff5a5a;font-size:15px;font-weight:800;\">"
								+ escapeHtml(seat) + "</span>");
			}
		} else {
			seatBadges.append("N/A");
		}
		// remove trailing separator look — join with commas instead
		String seatsLine = (seats != null && !seats.isEmpty()) ? String.join(", ", seats) : "N/A";

		return "<!DOCTYPE html><html><head><meta charset='utf-8'>"
				+ "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" + "</head>"
				+ "<body style=\"margin:0;padding:32px 12px;background-color:#0a0a0a;font-family:'Segoe UI',Arial,sans-serif;\">"
				+ "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='max-width:700px;margin:0 auto;'>"
				+ "<tr><td>"

				// Header brand line
				+ "<div style='text-align:center;margin-bottom:18px;'>"
				+ "  <span style='color:#ff8080;font-size:20px;font-weight:800;letter-spacing:0.5px;'>🎟️ "
				+ escapeHtml(appName) + "</span>"
				+ "  <div style='color:#4ade80;font-size:12px;font-weight:600;margin-top:4px;'>PAYMENT SUCCESSFUL · BOOKING CONFIRMED</div>"
				+ "</div>"

				// OUTER TICKET TABLE — 3 columns: dark card | perforation | white barcode panel
				+ "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' "
				+ "style='border-radius:16px;overflow:hidden;'><tr>"

				// ── LEFT: dark card (poster + details) ──────────────────────────
				+ "<td valign='top' style='background-color:#161616;padding:0;'>"
				+ "<table role='presentation' width='100%' cellpadding='0' cellspacing='0'><tr>"

				// poster
				+ "<td width='140' valign='top' style='padding:0;'>" + "<img src='" + posterUrl + "' width='140' alt='"
				+ escapeHtml(movieTitle) + "' "
				+ "style='width:140px;height:100%;min-height:230px;object-fit:cover;display:block;' />" + "</td>"

				// details
				+ "<td valign='top' style='padding:20px 22px;'>"
				+ "<table role='presentation' width='100%' cellpadding='0' cellspacing='0'><tr>" + "<td valign='top'>"
				+ "<div style='color:#ffffff;font-size:19px;font-weight:800;margin-bottom:8px;'>"
				+ escapeHtml(movieTitle) + "</div>" + "<div style='color:#999999;font-size:12px;'>"
				+ buildGenreLine(booking) + "</div>" + "</td>" + "<td valign='top' align='right'>"
				+ "<span style='display:inline-block;background:rgba(74,222,128,0.15);color:#4ade80;"
				+ "font-size:11px;font-weight:700;padding:5px 12px;border-radius:999px;white-space:nowrap;'>"
				+ escapeHtml(status) + "</span>" + "</td>" + "</tr></table>"

				+ "<div style='height:22px;'></div>"

				// Date / Time
				+ "<table role='presentation' width='100%' cellpadding='0' cellspacing='0'><tr>"
				+ "<td width='50%' valign='top'>"
				+ "<div style='color:#888888;font-size:10px;text-transform:uppercase;font-weight:700;letter-spacing:0.5px;margin-bottom:6px;'>Date</div>"
				+ "<div style='color:#ffffff;font-size:15px;font-weight:700;'>" + showDate + "</div>" + "</td>"
				+ "<td width='50%' valign='top'>"
				+ "<div style='color:#888888;font-size:10px;text-transform:uppercase;font-weight:700;letter-spacing:0.5px;margin-bottom:6px;'>Time</div>"
				+ "<div style='color:#ffffff;font-size:15px;font-weight:700;'>" + showTime + "</div>" + "</td>"
				+ "</tr></table>"

				+ "<div style='height:18px;'></div>"

				// Theater / Screen
				+ "<table role='presentation' width='100%' cellpadding='0' cellspacing='0'><tr>"
				+ "<td width='50%' valign='top'>"
				+ "<div style='color:#888888;font-size:10px;text-transform:uppercase;font-weight:700;letter-spacing:0.5px;margin-bottom:6px;'>Theater</div>"
				+ "<div style='color:#ffffff;font-size:15px;font-weight:700;'>" + escapeHtml(theaterName) + "</div>"
				+ "</td>" + "<td width='50%' valign='top'>"
				+ "<div style='color:#888888;font-size:10px;text-transform:uppercase;font-weight:700;letter-spacing:0.5px;margin-bottom:6px;'>Screen</div>"
				+ "<div style='color:#ffffff;font-size:15px;font-weight:700;'>Screen " + escapeHtml(screenName)
				+ "</div>" + "</td>" + "</tr></table>"

				+ "<div style='height:18px;'></div>" + "<div style='border-top:1px solid #2a2a2a;'></div>"
				+ "<div style='height:18px;'></div>"

				// Seats / Total Paid
				+ "<table role='presentation' width='100%' cellpadding='0' cellspacing='0'><tr>"
				+ "<td width='50%' valign='top'>"
				+ "<div style='color:#888888;font-size:10px;text-transform:uppercase;font-weight:700;letter-spacing:0.5px;margin-bottom:6px;'>Seats</div>"
				+ "<div style='color:#ff5a5a;font-size:16px;font-weight:800;'>" + escapeHtml(seatsLine) + "</div>"
				+ "</td>" + "<td width='50%' valign='top' align='right'>"
				+ "<div style='color:#888888;font-size:10px;text-transform:uppercase;font-weight:700;letter-spacing:0.5px;margin-bottom:6px;'>Total Paid</div>"
				+ "<div style='color:#ffffff;font-size:20px;font-weight:800;'>₹" + formatCurrency(totalPrice) + "</div>"
				+ "</td>" + "</tr></table>"

				+ "<div style='height:14px;'></div>" + "<div style='color:#666666;font-size:11px;'>Ref: " + bookingRef
				+ "</div>"

				+ "</td>" // end details cell
				+ "</tr></table>" + "</td>" // end left card cell

				// ── MIDDLE: perforation strip ────────────────────────────────────
				+ "<td width='2' style='background-color:#0a0a0a;'></td>"

				// ── RIGHT: white barcode panel ───────────────────────────────────
				+ "<td width='220' valign='middle' align='center' style='background-color:#1c1c1c;padding:24px 16px;'>"
				+ "<div style='background:#ffffff;display:inline-block;padding:16px;border-radius:12px;'>"
				+ "<img src='" + barcodeUrl + "' alt='" + bookingRef
				+ "' width='170' style='display:block;max-width:170px;' />"
				+ "<div style='color:#111111;font-size:11px;letter-spacing:1px;font-family:monospace;margin-top:8px;text-align:center;'>"
				+ bookingRef + "</div>" + "</div>"
				+ "<div style='color:#777777;font-size:10px;font-family:monospace;margin-top:14px;word-break:break-all;'>"
				+ transactionId + "</div>" + "</td>"

				+ "</tr></table>" // end outer ticket table

				// Footer
				+ "<div style='text-align:center;margin-top:22px;'>"
				+ "<div style='color:#ffffff;font-size:15px;font-weight:700;'>Enjoy your show!</div>"
				+ "<div style='color:#666666;font-size:11px;margin-top:4px;'>Please arrive 15 minutes before showtime</div>"
				+ "</div>"

				+ "</td></tr></table>" + "</body></html>";
	}
	// ─────────────────────────────────────────────────────────────────────────
	// HELPERS
	// ─────────────────────────────────────────────────────────────────────────

	/** Same tier boundaries as BookingService.getTier() / frontend SEAT_PRICING */
	private String getSeatTier(String seatId) {
		if (seatId == null || seatId.isEmpty())
			return "economy";
		char row = Character.toUpperCase(seatId.charAt(0));
		if (row <= 'B')
			return "economy";
		if (row <= 'J')
			return "standard";
		return "premium";
	}

	/** [borderColor, backgroundColor] — mirrors TIER_COLORS in the frontend */
	private String[] tierColors(String tier) {
		return switch (tier) {
		case "premium" -> new String[] { "#4ade80", "rgba(74,222,128,0.10)" }; // green
		case "standard" -> new String[] { "#facc15", "rgba(250,204,21,0.10)" }; // yellow
		default -> new String[] { "#ff8080", "rgba(255,128,128,0.10)" }; // primary/red
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
		try {
			return LocalDate.parse(d).format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"));
		} catch (Exception e) {
			return d != null ? d : "N/A";
		}
	}

	private String formatTime(String t) {
		if (t == null || t.isEmpty())
			return "N/A";
		try {
			if (t.contains("AM") || t.contains("PM"))
				return t;
			String[] p = t.split(":");
			int h = Integer.parseInt(p[0]);
			return (h % 12 == 0 ? 12 : h % 12) + ":" + p[1] + (h >= 12 ? " PM" : " AM");
		} catch (Exception e) {
			return t;
		}
	}

	private String getSafeString(String v, String f) {
		return (v != null && !v.isEmpty()) ? v : f;
	}

	private String formatCurrency(int a) {
		return String.format("%,d", a);
	}

	private String escapeHtml(String t) {
		return t == null ? "" : t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private String urlEncode(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8);
	}
}