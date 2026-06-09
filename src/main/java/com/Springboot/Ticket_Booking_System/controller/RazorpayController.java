package com.Springboot.Ticket_Booking_System.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Springboot.Ticket_Booking_System.service.RazorpayService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/razorpay")
@CrossOrigin(origins = "http://localhost:5173")
public class RazorpayController {

    @Autowired
    private RazorpayService razorpayService;

    // ─────────────────────────────────────────────────────────────────────────
    //  POST /api/razorpay/create-order
    //  Needs Clerk JWT — only signed-in users can start a payment
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String clerkUserId = (String) request.getAttribute("clerkUserId");
        if (clerkUserId == null) {
            System.err.println("❌ create-order: No clerkUserId — unauthorized");
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        int    amountInPaise = Integer.parseInt(String.valueOf(body.get("amount")));
        String receipt       = "receipt_" + System.currentTimeMillis();

        System.out.println("📦 Creating Razorpay order | user: " + clerkUserId
                + " | paise: " + amountInPaise);

        Map<String, Object> order = razorpayService.createOrder(amountInPaise, receipt);
        return ResponseEntity.ok(order);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  POST /api/razorpay/verify-payment
    //
    //  NO Clerk JWT check — intentional and correct.
    //
    //  The HMAC-SHA256 signature IS the security. It's computed with your
    //  Razorpay API secret, so it's impossible to forge. Adding a JWT check
    //  here causes 401 because after the Razorpay popup closes, the Clerk
    //  token context from the iframe handler() doesn't always survive.
    //
    //  This matches Razorpay's own official documentation pattern.
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/verify-payment")
    public ResponseEntity<Map<String, Object>> verifyPayment(
            @RequestBody Map<String, String> body) {

        String orderId   = body.get("razorpayOrderId");
        String paymentId = body.get("razorpayPaymentId");
        String signature = body.get("razorpaySignature");

        System.out.println("🔍 Verifying payment: " + paymentId);

        if (orderId == null || paymentId == null || signature == null) {
            System.err.println("❌ verify-payment: Missing fields");
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Missing required fields: orderId, paymentId, signature"
            ));
        }

        boolean isValid = razorpayService.verifySignature(orderId, paymentId, signature);

        if (isValid) {
            System.out.println("✅ Payment verified: " + paymentId);
            return ResponseEntity.ok(Map.of(
                "success",   true,
                "paymentId", paymentId,
                "message",   "Payment verified successfully"
            ));
        }

        System.err.println("❌ Signature mismatch for: " + paymentId);
        return ResponseEntity.status(400).body(Map.of(
            "success", false,
            "message", "Payment verification failed — invalid signature"
        ));
    }
}