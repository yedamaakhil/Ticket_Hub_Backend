package com.Springboot.Ticket_Booking_System.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.Springboot.Ticket_Booking_System.model.Booking;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
	List<Booking> findByClerkUserId(String clerkUserId);
}