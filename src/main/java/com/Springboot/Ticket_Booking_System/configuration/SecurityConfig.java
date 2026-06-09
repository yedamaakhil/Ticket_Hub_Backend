package com.Springboot.Ticket_Booking_System.configuration;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import com.Springboot.Ticket_Booking_System.filter.ClerkJwtFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private ClerkJwtFilter clerkJwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configure(http))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll() // ← temporarily allow all
            )
            .addFilterBefore(clerkJwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}