package com.Springboot.Ticket_Booking_System.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.Springboot.Ticket_Booking_System.model.BookedSeat;
import com.Springboot.Ticket_Booking_System.model.Show;
import com.Springboot.Ticket_Booking_System.repository.BookedSeatRepository;
import com.Springboot.Ticket_Booking_System.repository.ShowRepository;

@Service
public class SeatService {

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private BookedSeatRepository bookedSeatRepository;

    public List<String> getBookedSeats(Integer movieId, String date, String time) {
        LocalDate showDate = LocalDate.parse(date);
        Optional<Show> show = showRepository
            .findByMovieIdAndShowDateAndShowTime(movieId, showDate, time);

        if (show.isEmpty()) return List.of();

        return bookedSeatRepository.findByShowId(show.get().getId())
            .stream()
            .map(BookedSeat::getSeatId)
            .collect(Collectors.toList());
    }
}