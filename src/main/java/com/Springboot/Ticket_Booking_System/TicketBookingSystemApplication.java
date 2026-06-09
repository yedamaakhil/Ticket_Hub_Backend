package com.Springboot.Ticket_Booking_System;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TicketBookingSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(TicketBookingSystemApplication.class, args);
		System.out.println("Ticket_Booking_System.....!!!");
	}

}
