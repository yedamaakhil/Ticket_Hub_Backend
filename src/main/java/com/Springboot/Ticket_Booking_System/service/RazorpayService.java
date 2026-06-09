package com.Springboot.Ticket_Booking_System.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class RazorpayService {

    @Value("${razorpay.api.key}")
    private String apiKey;

    @Value("${razorpay.api.secret}")
    private String apiSecret;

    // ─────────────────────────────────────────────────────────────────────────
    //  Create a Razorpay Order
    //  amount is in PAISE (₹1 = 100 paise)
    // ─────────────────────────────────────────────────────────────────────────
    public Map<String, Object> createOrder(int amountInPaise, String receipt) {
        try {
            RazorpayClient client = new RazorpayClient(apiKey, apiSecret);

            JSONObject options = new JSONObject();
            options.put("amount",   amountInPaise);   // in paise
            options.put("currency", "INR");
            options.put("receipt",  receipt);
            options.put("payment_capture", 1);        // auto-capture

            Order order = client.orders.create(options);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId",  order.get("id"));
            response.put("amount",   order.get("amount"));
            response.put("currency", order.get("currency"));
            response.put("keyId",    apiKey);          // sent to frontend
            return response;

        } catch (RazorpayException e) {
            System.err.println("❌ Razorpay createOrder failed: " + e.getMessage());
            throw new RuntimeException("Failed to create Razorpay order: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Verify Razorpay payment signature (HMAC-SHA256)
    //
    //  Razorpay sends:  razorpay_order_id + "|" + razorpay_payment_id
    //  We HMAC that with our API secret and compare to razorpay_signature
    // ─────────────────────────────────────────────────────────────────────────
    public boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hashBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            StringBuilder hexHash = new StringBuilder();
            for (byte b : hashBytes) {
                hexHash.append(String.format("%02x", b));
            }

            boolean valid = hexHash.toString().equals(signature);
            System.out.println(valid
                ? "✅ Razorpay signature verified for payment: " + paymentId
                : "❌ Razorpay signature MISMATCH for payment: " + paymentId);
            return valid;

        } catch (Exception e) {
            System.err.println("❌ Signature verification error: " + e.getMessage());
            return false;
        }
    }
}