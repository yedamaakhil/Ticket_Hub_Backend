package com.Springboot.Ticket_Booking_System.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's @Scheduled task executor. Required for
 * SeatLockService.cleanupExpiredLocks().
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
	// no extra beans needed — @EnableScheduling is sufficient
}