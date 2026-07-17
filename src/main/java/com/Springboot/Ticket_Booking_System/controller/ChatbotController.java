package com.Springboot.Ticket_Booking_System.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import com.Springboot.Ticket_Booking_System.model.Booking;
import com.Springboot.Ticket_Booking_System.repository.BookingRepository;
import com.Springboot.Ticket_Booking_System.repository.ShowRepository;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:5173")
public class ChatbotController {

    @Value("${anthropic.api.key}")
    private String anthropicApiKey;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ShowRepository showRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String CLAUDE_MODEL    = "claude-sonnet-4-6";

    // ── POST /api/chat ──────────────────────────────────────────────────────
    // Body: { messages: [{role, content}], clerkUserId?: string }
    @PostMapping
    public ResponseEntity<Map<String, Object>> chat(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> messages =
                (List<Map<String, String>>) body.get("messages");

            // Optional: get user's booking history for personalised answers
            String clerkUserId = (String) request.getAttribute("clerkUserId");
            String bookingContext = buildBookingContext(clerkUserId);

            // ── Build system prompt with live DB data ──────────────────────
            String systemPrompt = buildSystemPrompt(bookingContext);

            // ── Build Claude API request ───────────────────────────────────
            Map<String, Object> claudeRequest = new HashMap<>();
            claudeRequest.put("model",      CLAUDE_MODEL);
            claudeRequest.put("max_tokens", 500);
            claudeRequest.put("system",     systemPrompt);
            claudeRequest.put("messages",   messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key",         anthropicApiKey);
            headers.set("anthropic-version",  "2023-06-01");

            HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(claudeRequest, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> claudeResponse = restTemplate.postForObject(
                CLAUDE_API_URL, entity, Map.class);

            // ── Extract text from response ─────────────────────────────────
            String reply = extractReply(claudeResponse);

            Map<String, Object> result = new HashMap<>();
            result.put("reply",   reply);
            result.put("success", true);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("❌ Chatbot error: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("reply",   "I'm having trouble right now. Please try again in a moment.");
            return ResponseEntity.ok(err); // always 200 so frontend handles gracefully
        }
    }

    // ── Extract text content from Claude response ──────────────────────────
    @SuppressWarnings("unchecked")
    private String extractReply(Map<String, Object> response) {
        if (response == null) return "Sorry, I could not get a response.";
        List<Map<String, Object>> content =
            (List<Map<String, Object>>) response.get("content");
        if (content == null || content.isEmpty()) return "Sorry, I could not get a response.";
        Object text = content.get(0).get("text");
        return text != null ? text.toString() : "Sorry, I could not get a response.";
    }

    // ── Get user's booking history from DB ────────────────────────────────
    private String buildBookingContext(String clerkUserId) {
        if (clerkUserId == null || clerkUserId.isBlank()) return "";
        try {
            List<Booking> bookings = bookingRepository.findByClerkUserId(clerkUserId);
            if (bookings.isEmpty()) return "This user has no bookings yet.";

            StringBuilder sb = new StringBuilder("This user's booking history:\n");
            bookings.stream().limit(5).forEach(b -> sb
                .append("  • ").append(b.getMovieTitle())
                .append(" on ").append(b.getShowDate())
                .append(" at ").append(b.getShowTime())
                .append(", Seats: ").append(b.getSeats())
                .append(", Ref: ").append(b.getBookingRef())
                .append(", Status: ").append(b.getStatus())
                .append("\n"));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // ── Full system prompt ─────────────────────────────────────────────────
    private String buildSystemPrompt(String bookingContext) {
        return """
You are TixBot, the friendly AI assistant for TixRush — an Indian movie ticket booking platform.

## Your personality
- Warm, enthusiastic about movies, concise and helpful
- Use emojis naturally but sparingly 🎬🎟️
- Always respond in the same language the user writes in
- Keep answers concise unless the user asks for detail

## Movies currently in our database

1. Kalki 2898 AD (2024) | Action, Sci-Fi, Fantasy | 3h 0m | ⭐8.2 | Prabhas, Amitabh Bachchan, Kamal Haasan, Deepika Padukone | Telugu | AVD Cinemas Screen 1
2. Pushpa 2: The Rule (2024) | Action, Crime, Thriller | 3h 20m | ⭐7.9 | Allu Arjun, Rashmika Mandanna, Fahadh Faasil | Telugu | AVD Cinemas Screen 2
3. Devara: Part 1 (2024) | Action, Adventure, Drama | 2h 45m | ⭐7.5 | NTR Jr., Saif Ali Khan, Janhvi Kapoor | Telugu | AVD Cinemas Screen 3
4. Salaar: Part 2 (2024) | Action, Drama, Thriller | 2h 55m | ⭐8.0 | Prabhas, Prithviraj Sukumaran, Shruti Haasan | Telugu | AVD Cinemas Screen 1
5. Game Changer (2025) | Action, Drama, Thriller | 2h 48m | ⭐7.2 | Ram Charan, Kiara Advani | Telugu | PVR Cinemas Screen 1
6. OG / They Call Him OG (2025) | Action, Crime, Drama | 2h 40m | ⭐7.6 | Pawan Kalyan, Priyanka Mohan, Emraan Hashmi | Telugu | PVR Cinemas Screen 2
7. Kingdom (2025) | Action, Fantasy, History | 2h 30m | ⭐7.8 | Vijay Deverakonda, Bhagyashri Borse | Telugu | PVR Cinemas Screen 3
8. Dhurandhar The Revenge (2026) | Action, Drama, History | 4h 5m | ⭐9.2 | Ranveer Singh, Sanjay Dutt, R. Madhavan | Telugu | PVR Cinemas Screen 4
9. KGF Chapter 2 (2022) | Action, Fantasy, Drama | 2h 40m | ⭐8.8 | Yash, Sanjay Dutt, Raveena Tandon | Telugu | INOX Leisure Screen 1
10. Arjun Reddy (2018) | Drama | 2h 45m | ⭐8.5 | Vijay Deverakonda, Shalini Pandey | Telugu | INOX Leisure Screen 2
11. Dacoit (2026) | Action, Adventure, Thriller | 2h 45m | ⭐8.5 | Adivi Sesh, Mrunal Thakur | Telugu | INOX Leisure Screen 3
12. Hanu-Man (2024) | Action, Fantasy | 2h 38m | ⭐8.4 | Teja Sajja, Amritha Aiyer | Telugu | Cinepolis Screen 1
13. Animal (2023) | Action, Drama | 3h 21m | ⭐7.0 | Ranbir Kapoor, Rashmika Mandanna, Bobby Deol | Hindi | Cinepolis Screen 2
14. RRR (2022) | Action, Drama | 3h 2m | ⭐8.8 | NTR Jr., Ram Charan, Alia Bhatt, Ajay Devgn | Telugu | Cinepolis Screen 3

## Show timings
Date 2026-04-10: 10:00 AM – Kalki 2898 AD, 1:30 PM – Pushpa 2, 4:00 PM – Devara, 6:30 PM – Salaar 2, 9:00 PM – Game Changer
Date 2026-04-11: 10:00 AM – OG, 1:30 PM – Kingdom, 4:00 PM – Dhurandhar, 6:30 PM – KGF 2, 9:00 PM – Arjun Reddy
Date 2026-04-12: 10:00 AM – Kalki 2898 AD, 1:30 PM – Pushpa 2, 4:00 PM – Devara, 6:30 PM – Salaar 2, 9:00 PM – Game Changer
Date 2026-04-13: 10:00 AM – OG, 1:30 PM – Kingdom, 4:00 PM – Dhurandhar, 6:30 PM – KGF 2, 9:00 PM – Arjun Reddy
Date 2026-04-14: 10:00 AM – Kalki 2898 AD, 1:30 PM – Pushpa 2, 4:00 PM – Devara, 6:30 PM – Salaar 2, 9:00 PM – Game Changer

## Theaters
- AVD Cinemas (Screens 1–3, Hyderabad)
- PVR Cinemas (Screens 1–4, Hyderabad)
- INOX Leisure (Screens 1–3, Hyderabad)
- Cinepolis (Screens 1–3, Hyderabad)

## Ticket pricing
| Tier     | Rows | Price    |
|----------|------|----------|
| Economy  | A–B  | ₹150/seat|
| Standard | C–J  | ₹300/seat|
| Premium  | K–R  | ₹500/seat|

Taxes added at checkout: GST 8% + Cinema Development Tax 2% + Convenience fee ₹13/seat (+ 8% GST on conv fee)

## How to book on TixRush
1. Click Movies in the navbar → browse or search
2. Click on a movie → see cast, rating, synopsis
3. Select a date and show time
4. Pick seats on the interactive seat map (max 6 per booking)
5. Review price breakdown → click Pay via Razorpay
6. Pay with UPI / Credit Card / Debit Card / Net Banking / Wallet
7. Booking confirmed instantly + confirmation email with ticket barcode sent

## Cancellation & support
- Cancellations can be done from My Bookings page
- Booking reference starts with BK (e.g. BK1718234567890)

""" + (bookingContext.isBlank() ? "" : "\n## This user's bookings\n" + bookingContext) + """

## What you should do
- Recommend movies based on genre, mood, cast, language, or rating
- Tell exactly which movies are playing on a specific date and time
- Calculate and explain ticket pricing including taxes if asked
- Guide users through the booking process step by step
- Answer questions about theaters, screens, and seat types
- Reference the user's own bookings if they ask about "my bookings"
- Always be helpful, warm, and concise (under 150 words unless detail is needed)
- If you don't know something specific, say so honestly and suggest they check the website
""";
    }
}