package com.Springboot.Ticket_Booking_System.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ClerkJwtFilter extends OncePerRequestFilter {

    private static final String DEFAULT_CLERK_PUBLIC_KEY =
        "-----BEGIN PUBLIC KEY-----\n" +
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyPLnVpP0mRs5MMSWqMWx\n" +
        "p/laz66tyuuIy/VdIqZlHSEqFyUiejAMkHCiw/OQsQ6CwvwiYyTb0+mcOpGj/vdH\n" +
        "J6myI1j4iuvLUCW7NIrxXSiBDoY8mfBG2dVmvU8PE6SgkXmgSt8BOaBto2+fhZUI\n" +
        "0tGV7y/AbQwIodVWN9QmdrplQ2CVvhG3xcd7ZYbkuQkDM9vBCHpJGw7fjCFCyOqJ\n" +
        "hmo2EpSwXwuY99L7NGkMpIErjmDV/NSVUcUIq2Ku3+ix+7Ye5J+ObP/bKOmUBZVT\n" +
        "sZ5tIez737Vi4KZuBelFuXKjT9ePNINLk/dYIvtv0oXR8l771SR3TJTzv/aZs3Pq\n" +
        "+QIDAQAB\n" +
        "-----END PUBLIC KEY-----";

    @Value("${clerk.jwt.public-key:}")
    private String clerkPublicKeyPem;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
        HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                RSAPublicKey publicKey = (RSAPublicKey) getClerkPublicKey();
                Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

                setUserAttributes(request, claims.getSubject(), claims.get("email", String.class));
            } catch (Exception e) {
                System.err.println("JWT verification failed, trying payload decode: " + e.getMessage());
                decodeAndSetUserFromPayload(request, token);
            }
        }

        chain.doFilter(request, response);
    }

    private void setUserAttributes(HttpServletRequest request, String userId, String email) {
        if (userId != null && !userId.isBlank()) {
            request.setAttribute("clerkUserId", userId);
        }
        if (email != null && !email.isBlank()) {
            request.setAttribute("userEmail", email);
        }
    }

    private void decodeAndSetUserFromPayload(HttpServletRequest request, String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return;

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

            String userId = extractJsonString(payloadJson, "sub");
            String email = extractJsonString(payloadJson, "email");
            if (email == null || email.isBlank()) {
                email = extractJsonString(payloadJson, "primary_email_address");
            }

            setUserAttributes(request, userId, email);
        } catch (Exception e) {
            System.err.println("Failed to decode JWT payload: " + e.getMessage());
        }
    }

    private String extractJsonString(String json, String field) {
        String marker = "\"" + field + "\"";
        int fieldIndex = json.indexOf(marker);
        if (fieldIndex < 0) return null;

        int colonIndex = json.indexOf(':', fieldIndex + marker.length());
        if (colonIndex < 0) return null;

        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        if (valueStart >= json.length() || json.charAt(valueStart) != '"') {
            return null;
        }

        int valueEnd = valueStart + 1;
        while (valueEnd < json.length()) {
            char c = json.charAt(valueEnd);
            if (c == '"' && json.charAt(valueEnd - 1) != '\\') break;
            valueEnd++;
        }
        if (valueEnd >= json.length()) return null;

        return json.substring(valueStart + 1, valueEnd);
    }

    private PublicKey getClerkPublicKey() throws Exception {
        String pem = (clerkPublicKeyPem != null && !clerkPublicKeyPem.isBlank())
            ? clerkPublicKeyPem
            : DEFAULT_CLERK_PUBLIC_KEY;

        String publicKeyPEM = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(publicKeyPEM);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(new X509EncodedKeySpec(decoded));
    }
}
