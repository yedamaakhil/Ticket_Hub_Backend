package com.Springboot.Ticket_Booking_System.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Springboot.Ticket_Booking_System.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

}