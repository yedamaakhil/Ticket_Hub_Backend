package com.Springboot.Ticket_Booking_System.repository;

import java.util.Collection;

import com.Springboot.Ticket_Booking_System.dto.UserDTO;

public interface UserRepository {

	Collection<UserDTO> findAll();

}
