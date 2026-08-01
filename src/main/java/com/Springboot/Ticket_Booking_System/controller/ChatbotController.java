package com.Springboot.Ticket_Booking_System.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.Springboot.Ticket_Booking_System.model.Booking;
import com.Springboot.Ticket_Booking_System.repository.BookingRepository;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")   // allow all — your CorsConfig already restricts properly
public class ChatbotController {

    // ── injected from application.properties / Render env var ─────────────
    @Value("${anthropic.api.key:}")
    private String anthropicApiKey;

    @Autowired
    private BookingRepository bookingRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    // ── Use a real, currently available Claude model ───────────────────────
    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String CLAUDE_MODEL   = "claude-sonnet-4-6";   // ← FIXED

    // ─────────────────────────────────────────────────────────────────────────
    //  POST /api/chat
    //  Body: { messages: [{role, content}], clerkUserId?: string }
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<Map<String, Object>> chat(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        // ── 1. Guard: API key must be set ─────────────────────────────────
        if (anthropicApiKey == null || anthropicApiKey.isBlank()) {
            System.err.println("❌ ANTHROPIC_API_KEY is not set in environment variables!");
            response.put("success", false);
            response.put("reply",
                "The AI assistant is not configured yet. Please contact support.");
            return ResponseEntity.ok(response);
        }

        // ── 2. Extract messages ───────────────────────────────────────────
        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages =
            (List<Map<String, String>>) body.get("messages");

        if (messages == null || messages.isEmpty()) {
            response.put("success", false);
            response.put("reply", "No message received. Please try again.");
            return ResponseEntity.ok(response);
        }

        // ── 3. Build user booking context (if signed in) ──────────────────
        String clerkUserId = (String) request.getAttribute("clerkUserId");
        if (clerkUserId == null) clerkUserId = (String) body.get("clerkUserId");
        String bookingCtx = buildBookingContext(clerkUserId);

        // ── 4. Call Claude API ────────────────────────────────────────────
        try {
            Map<String, Object> claudeReq = new HashMap<>();
            claudeReq.put("model",      CLAUDE_MODEL);
            claudeReq.put("max_tokens", 600);
            claudeReq.put("system",     buildSystemPrompt(bookingCtx));
            claudeReq.put("messages",   messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key",        anthropicApiKey);
            headers.set("anthropic-version", "2023-06-01");

            @SuppressWarnings("unchecked")
            Map<String, Object> claudeRes = restTemplate.postForObject(
                CLAUDE_API_URL,
                new HttpEntity<>(claudeReq, headers),
                Map.class
            );

            String reply = extractText(claudeRes);
            System.out.println("✅ Chatbot replied successfully.");

            response.put("success", true);
            response.put("reply",   reply);
            return ResponseEntity.ok(response);

        } catch (HttpClientErrorException e) {
            // 4xx from Anthropic — e.g. wrong key, bad request
            System.err.println("❌ Anthropic 4xx: " + e.getStatusCode() + " — " + e.getResponseBodyAsString());
            response.put("success", false);
            response.put("reply",   "AI service returned an error (" + e.getStatusCode() + "). Please check your API key.");
            return ResponseEntity.ok(response);

        } catch (HttpServerErrorException e) {
            // 5xx from Anthropic — rare
            System.err.println("❌ Anthropic 5xx: " + e.getStatusCode());
            response.put("success", false);
            response.put("reply",   "The AI service is temporarily unavailable. Please try again shortly.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Chatbot unexpected error: " + e.getClass().getSimpleName() + " — " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("reply",   "Something went wrong on our end. Please try again.");
            return ResponseEntity.ok(response);
        }
    }

    // ── Extract text from Claude's response payload ────────────────────────
    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> res) {
        if (res == null) return "No response from AI.";
        try {
            List<Map<String, Object>> content = (List<Map<String, Object>>) res.get("content");
            if (content == null || content.isEmpty()) return "Empty response from AI.";
            Object text = content.get(0).get("text");
            return text != null ? text.toString() : "Could not read AI response.";
        } catch (Exception e) {
            return "Error reading AI response.";
        }
    }

    // ── Get user's booking history from DB ────────────────────────────────
    private String buildBookingContext(String clerkUserId) {
        if (clerkUserId == null || clerkUserId.isBlank()) return "";
        try {
            List<Booking> bookings = bookingRepository.findByClerkUserId(clerkUserId);
            if (bookings.isEmpty()) return "This user has no bookings yet.";
            StringBuilder sb = new StringBuilder();
            bookings.stream().limit(5).forEach(b ->
                sb.append("• ").append(b.getMovieTitle())
                  .append(" | Date: ").append(b.getShowDate())
                  .append(" | Time: ").append(b.getShowTime())
                  .append(" | Seats: ").append(b.getSeats())
                  .append(" | Ref: ").append(b.getBookingRef())
                  .append(" | Status: ").append(b.getStatus())
                  .append("\n")
            );
            return sb.toString();
        } catch (Exception e) {
            System.err.println("⚠️ Could not fetch bookings: " + e.getMessage());
            return "";
        }
    }

    // ── System prompt ──────────────────────────────────────────────────────
    private String buildSystemPrompt(String bookingContext) {

        String movies = """
1.  Kalki 2898 AD (2024)            | Action, Sci-Fi, Fantasy    | 3h 0m  | ⭐8.2 | Prabhas, Amitabh Bachchan, Kamal Haasan, Deepika Padukone | Telugu | AVD Cinemas Screen 1
2.  Pushpa 2: The Rule (2024)       | Action, Crime, Thriller    | 3h 20m | ⭐7.9 | Allu Arjun, Rashmika Mandanna, Fahadh Faasil              | Telugu | AVD Cinemas Screen 2
3.  Devara: Part 1 (2024)           | Action, Adventure, Drama   | 2h 45m | ⭐7.5 | NTR Jr., Saif Ali Khan, Janhvi Kapoor                    | Telugu | AVD Cinemas Screen 3
4.  Salaar: Part 2 (2024)           | Action, Drama, Thriller    | 2h 55m | ⭐8.0 | Prabhas, Prithviraj Sukumaran, Shruti Haasan              | Telugu | AVD Cinemas Screen 1
5.  Game Changer (2025)             | Action, Drama, Thriller    | 2h 48m | ⭐7.2 | Ram Charan, Kiara Advani                                 | Telugu | PVR Cinemas Screen 1
6.  OG / They Call Him OG (2025)    | Action, Crime, Drama       | 2h 40m | ⭐7.6 | Pawan Kalyan, Priyanka Mohan, Emraan Hashmi               | Telugu | PVR Cinemas Screen 2
7.  Kingdom (2025)                  | Action, Fantasy, History   | 2h 30m | ⭐7.8 | Vijay Deverakonda, Bhagyashri Borse                      | Telugu | PVR Cinemas Screen 3
8.  Dhurandhar The Revenge (2026)   | Action, Drama, History     | 4h 5m  | ⭐9.2 | Ranveer Singh, Sanjay Dutt, R. Madhavan                  | Telugu | PVR Cinemas Screen 4
9.  KGF Chapter 2 (2022)            | Action, Fantasy, Drama     | 2h 40m | ⭐8.8 | Yash, Sanjay Dutt, Raveena Tandon                        | Telugu | INOX Leisure Screen 1
10. Arjun Reddy (2018)              | Drama                      | 2h 45m | ⭐8.5 | Vijay Deverakonda, Shalini Pandey                        | Telugu | INOX Leisure Screen 2
11. Dacoit (2026)                   | Action, Thriller           | 2h 45m | ⭐8.5 | Adivi Sesh, Mrunal Thakur                                | Telugu | INOX Leisure Screen 3
12. Hanu-Man (2024)                 | Action, Fantasy            | 2h 38m | ⭐8.4 | Teja Sajja, Amritha Aiyer                                | Telugu | Cinepolis Screen 1
13. Animal (2023)                   | Action, Drama              | 3h 21m | ⭐7.0 | Ranbir Kapoor, Rashmika Mandanna, Bobby Deol             | Hindi  | Cinepolis Screen 2
14. RRR (2022)                      | Action, Drama              | 3h 2m  | ⭐8.8 | NTR Jr., Ram Charan, Alia Bhatt, Ajay Devgn              | Telugu | Cinepolis Screen 3
""";

        String showtimes = """
2026-04-10: 10:00 AM – Kalki 2898 AD | 1:30 PM – Pushpa 2 | 4:00 PM – Devara | 6:30 PM – Salaar 2 | 9:00 PM – Game Changer
2026-04-11: 10:00 AM – OG            | 1:30 PM – Kingdom  | 4:00 PM – Dhurandhar | 6:30 PM – KGF 2    | 9:00 PM – Arjun Reddy
2026-04-12: 10:00 AM – Kalki 2898 AD | 1:30 PM – Pushpa 2 | 4:00 PM – Devara | 6:30 PM – Salaar 2 | 9:00 PM – Game Changer
2026-04-13: 10:00 AM – OG            | 1:30 PM – Kingdom  | 4:00 PM – Dhurandhar | 6:30 PM – KGF 2    | 9:00 PM – Arjun Reddy
2026-04-14: 10:00 AM – Kalki 2898 AD | 1:30 PM – Pushpa 2 | 4:00 PM – Devara | 6:30 PM – Salaar 2 | 9:00 PM – Game Changer
""";

        String base = """
You are TixBot, the friendly AI assistant for TicketHub — an Indian movie ticket booking platform based in Hyderabad.

## Personality
- Warm, enthusiastic about movies, helpful and concise
- Use emojis naturally but sparingly 🎬🎟️
- Reply in the same language the user writes in
- Keep answers under 150 words unless detail is requested

## Movies in our database
""" + movies + """

## Showtimes
""" + showtimes + """

## Theaters (all in Hyderabad)
- AVD Cinemas — Screens 1, 2, 3
- PVR Cinemas — Screens 1, 2, 3, 4
- INOX Leisure — Screens 1, 2, 3
- Cinepolis    — Screens 1, 2, 3

## Ticket pricing
- Economy  (Rows A–B): ₹150/seat
- Standard (Rows C–J): ₹300/seat
- Premium  (Rows K–R): ₹500/seat
- Taxes: GST 8% + Cinema Dev Tax 2% + Convenience fee ₹13/seat (+ 8% GST)

## Booking steps
1. Click Movies → choose a movie
2. Select date and show time
3. Pick seats (max 6 per booking)
4. Pay via Razorpay (UPI, Card, Net Banking, Wallet, EMI)
5. Instant confirmation + email ticket with barcode

## Cancellations
- Go to My Bookings → cancel from there
- Booking refs start with BK (e.g. BK1718234567890)

## Your job
- Recommend movies by genre, mood, cast, language, rating
- Tell exactly what's playing on a given date/time
- Explain pricing and taxes clearly
- Guide users step by step through booking
- If user asks about "my bookings" use the booking history below
- Be honest when you don't know something; suggest checking the website
""";

        return bookingContext.isBlank()
            ? base
            : base + "\n## This user's booking history\n" + bookingContext;
    }
}