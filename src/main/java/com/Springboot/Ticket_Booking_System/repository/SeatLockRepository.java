package com.Springboot.Ticket_Booking_System.repository;

import com.Springboot.Ticket_Booking_System.model.SeatLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SeatLockRepository extends JpaRepository<SeatLock, Long> {

	/** All non-expired locks for a show */
	@Query("SELECT sl FROM SeatLock sl WHERE sl.showId = :showId AND sl.expiresAt > :now")
	List<SeatLock> findActiveByShowId(@Param("showId") Long showId, @Param("now") LocalDateTime now);

	/**
	 * Delete every lock held by a session for a show (payment success / release)
	 */
	@Modifying
	@Query("DELETE FROM SeatLock sl WHERE sl.showId = :showId AND sl.sessionId = :sessionId")
	void deleteByShowIdAndSessionId(@Param("showId") Long showId, @Param("sessionId") String sessionId);

	/** Scheduled cleanup — sweep all expired locks */
	@Modifying
	@Query("DELETE FROM SeatLock sl WHERE sl.expiresAt <= :now")
	int deleteExpired(@Param("now") LocalDateTime now);
}