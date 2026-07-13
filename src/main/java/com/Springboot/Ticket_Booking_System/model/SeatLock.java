package com.Springboot.Ticket_Booking_System.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Temporary seat reservation held while a user is selecting seats or going
 * through the Razorpay payment flow. Locks expire after LOCK_DURATION_MINUTES
 * and are swept clean by SeatLockService's scheduled cleanup task.
 */
@Entity
@Table(name = "seat_locks", indexes = { @Index(name = "idx_seat_lock_show", columnList = "show_id"),
		@Index(name = "idx_seat_lock_session", columnList = "session_id"),
		@Index(name = "idx_seat_lock_expires", columnList = "expires_at") })
public class SeatLock {

	public static final int LOCK_DURATION_MINUTES = 10;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "show_id", nullable = false)
	private Long showId;

	/** One row per seat (A1, B3, …) */
	@Column(name = "seat_number", nullable = false, length = 10)
	private String seatNumber;

	/**
	 * Identifies the session holding this lock. Use Clerk userId when
	 * authenticated, or a UUID stored in sessionStorage otherwise.
	 */
	@Column(name = "session_id", nullable = false, length = 128)
	private String sessionId;

	@Column(name = "locked_at", nullable = false)
	private LocalDateTime lockedAt;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	public SeatLock() {
	}

	public SeatLock(Long showId, String seatNumber, String sessionId) {
		this.showId = showId;
		this.seatNumber = seatNumber;
		this.sessionId = sessionId;
		this.lockedAt = LocalDateTime.now();
		this.expiresAt = this.lockedAt.plusMinutes(LOCK_DURATION_MINUTES);
	}

	public boolean isExpired() {
		return LocalDateTime.now().isAfter(expiresAt);
	}

	// Getters
	public Long getId() {
		return id;
	}

	public Long getShowId() {
		return showId;
	}

	public String getSeatNumber() {
		return seatNumber;
	}

	public String getSessionId() {
		return sessionId;
	}

	public LocalDateTime getLockedAt() {
		return lockedAt;
	}

	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}

	// Setters
	public void setShowId(Long showId) {
		this.showId = showId;
	}

	public void setSeatNumber(String seatNumber) {
		this.seatNumber = seatNumber;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public void setLockedAt(LocalDateTime lockedAt) {
		this.lockedAt = lockedAt;
	}

	public void setExpiresAt(LocalDateTime expires) {
		this.expiresAt = expires;
	}
}