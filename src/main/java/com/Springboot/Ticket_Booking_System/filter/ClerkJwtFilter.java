package com.Springboot.Ticket_Booking_System.filter;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

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

    private static final String CLERK_PUBLIC_KEY =
        "-----BEGIN PUBLIC KEY-----\n" +
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyPLnVpP0mRs5MMSWqMWx\n" +
        "p/laz66tyuuIy/VdIqZlHSEqFyUiejAMkHCiw/OQsQ6CwvwiYyTb0+mcOpGj/vdH\n" +
        "J6myI1j4iuvLUCW7NIrxXSiBDoY8mfBG2dVmvU8PE6SgkXmgSt8BOaBto2+fhZUI\n" +
        "0tGV7y/AbQwIodVWN9QmdrplQ2CVvhG3xcd7ZYbkuQkDM9vBCHpJGw7fjCFCyOqJ\n" +
        "hmo2EpSwXwuY99L7NGkMpIErjmDV/NSVUcUIq2Ku3+ix+7Ye5J+ObP/bKOmUBZVT\n" +
        "sZ5tIez737Vi4KZuBelFuXKjT9ePNINLk/dYIvtv0oXR8l771SR3TJTzv/aZs3Pq\n" +
        "+QIDAQAB\n" +
        "-----END PUBLIC KEY-----";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
        HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        String path = request.getRequestURI();
        String authHeader = request.getHeader("Authorization");
        
        // Log for debugging
        System.out.println("=== Filter Debug ===");
        System.out.println("Path: " + path);
        System.out.println("Auth Header Present: " + (authHeader != null));
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            System.out.println("Token length: " + token.length());
            System.out.println("Token prefix: " + token.substring(0, Math.min(50, token.length())) + "...");
            
            try {
                // Get public key
                RSAPublicKey publicKey = (RSAPublicKey) getClerkPublicKey();
                System.out.println("Public key loaded successfully");
                
                // Parse and verify JWT
                Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

                String userId = claims.getSubject();
                System.out.println("✅ JWT verified successfully!");
                System.out.println("User ID from token: " + userId);
                
                // Also get email if available
                String email = claims.get("email", String.class);
                System.out.println("Email from token: " + email);
                
                request.setAttribute("clerkUserId", userId);
                request.setAttribute("userEmail", email);

            } catch (Exception e) {
                System.err.println("❌ JWT verification error: " + e.getMessage());
                e.printStackTrace();
                // Don't block the request, but log the error
                // The controller will handle missing userId
            }
        } else {
            System.out.println("⚠️ No Bearer token found in request");
            System.out.println("Auth header value: " + authHeader);
        }
        
        System.out.println("=== End Filter Debug ===\n");

        chain.doFilter(request, response);
    }

    private PublicKey getClerkPublicKey() throws Exception {
        String publicKeyPEM = CLERK_PUBLIC_KEY
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(publicKeyPEM);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(new X509EncodedKeySpec(decoded));
    }
}