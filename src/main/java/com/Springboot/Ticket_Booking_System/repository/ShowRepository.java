package com.Springboot.Ticket_Booking_System.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Springboot.Ticket_Booking_System.model.Show;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

    // Used by old code — kept for backward compat, but throws
    // IncorrectResultSizeDataAccessException if duplicate rows exist.
    Optional<Show> findByMovieIdAndShowDateAndShowTime(
        Integer movieId, LocalDate showDate, String showTime
    );

    // ★ Safe version — returns the first match instead of throwing when
    //   duplicate show rows exist for the same (movieId, date, time).
    //   Used by SeatLockController and BookingService.
    Optional<Show> findFirstByMovieIdAndShowDateAndShowTime(
        Integer movieId, LocalDate showDate, String showTime
    );

    List<Show> findByShowDate(LocalDate showDate);
}