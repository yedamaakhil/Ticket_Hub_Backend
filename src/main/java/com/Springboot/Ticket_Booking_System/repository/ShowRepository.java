package com.Springboot.Ticket_Booking_System.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Springboot.Ticket_Booking_System.model.Show;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {
    Optional<Show> findByMovieIdAndShowDateAndShowTime(
        Integer movieId, LocalDate showDate, String showTime
    );

	List<Show> findByShowDate(LocalDate showDate);
}