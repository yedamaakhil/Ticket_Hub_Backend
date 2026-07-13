package com.Springboot.Ticket_Booking_System.service;

import com.Springboot.Ticket_Booking_System.model.SeatLock;
import com.Springboot.Ticket_Booking_System.repository.SeatLockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SeatLockService {

	@Autowired
	private SeatLockRepository seatLockRepository;

	// ─────────────────────────────────────────────────────────────────────────
	// Lock seats
	//
	// Replaces the entire lock set for (showId, sessionId) atomically with
	// the new seats list. Passing an empty list effectively releases all locks.
	// ─────────────────────────────────────────────────────────────────────────
	@Transactional
	public void updateLocks(Long showId, List<String> seats, String sessionId) {
		// Remove previous locks for this session
		seatLockRepository.deleteByShowIdAndSessionId(showId, sessionId);

		if (seats == null || seats.isEmpty())
			return;

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime expires = now.plusMinutes(SeatLock.LOCK_DURATION_MINUTES);

		for (String seat : seats) {
			SeatLock lock = new SeatLock();
			lock.setShowId(showId);
			lock.setSeatNumber(seat);
			lock.setSessionId(sessionId);
			lock.setLockedAt(now);
			lock.setExpiresAt(expires);
			seatLockRepository.save(lock);
		}
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Release all locks for a session on a show
	// Called after payment success or on page unmount.
	// ─────────────────────────────────────────────────────────────────────────
	@Transactional
	public void releaseLocks(Long showId, String sessionId) {
		seatLockRepository.deleteByShowIdAndSessionId(showId, sessionId);
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Seat status for a show
	//
	// Returns two lists:
	// lockedByOthers — seats held by someone else → shown as unavailable
	// myLocks — seats held by this session → shown as selected
	// ─────────────────────────────────────────────────────────────────────────
	public Map<String, List<String>> getSeatStatus(Long showId, String sessionId) {
		List<SeatLock> active = seatLockRepository.findActiveByShowId(showId, LocalDateTime.now());

		List<String> lockedByOthers = active.stream().filter(l -> !l.getSessionId().equals(sessionId))
				.map(SeatLock::getSeatNumber).distinct().collect(Collectors.toList());

		List<String> myLocks = active.stream().filter(l -> l.getSessionId().equals(sessionId))
				.map(SeatLock::getSeatNumber).collect(Collectors.toList());

		return Map.of("lockedByOthers", lockedByOthers, "myLocks", myLocks);
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Scheduled cleanup — removes expired locks every 60 seconds.
	// Requires @EnableScheduling on your main class or a @Configuration class.
	// ─────────────────────────────────────────────────────────────────────────
	@Scheduled(fixedDelay = 60_000)
	@Transactional
	public void cleanupExpiredLocks() {
		int deleted = seatLockRepository.deleteExpired(LocalDateTime.now());
		if (deleted > 0) {
			System.out.println("🧹 Removed " + deleted + " expired seat lock(s)");
		}
	}
}