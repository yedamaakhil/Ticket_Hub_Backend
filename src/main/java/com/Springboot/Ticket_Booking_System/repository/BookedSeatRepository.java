package com.Springboot.Ticket_Booking_System.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Springboot.Ticket_Booking_System.model.BookedSeat;

@Repository
public interface BookedSeatRepository extends JpaRepository<BookedSeat, Long> {
    List<BookedSeat> findByShowId(Long showId);
    boolean existsByShowIdAndSeatId(Long showId, String seatId);
    
//------------------- Admin ------------------------------------
    
 // Add this method to your BookedSeatRepository
    long countByShowId(Long showId);
}