package com.Springboot.Ticket_Booking_System.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Springboot.Ticket_Booking_System.model.Movie;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    // No custom queries needed yet — JpaRepository already provides
    // findAll(), save(), findById(), deleteById(), existsById()
    // which is everything MovieService currently calls.
}