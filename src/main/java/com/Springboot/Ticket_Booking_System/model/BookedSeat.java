package com.Springboot.Ticket_Booking_System.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "booked_seats",uniqueConstraints = {@UniqueConstraint(columnNames = {"show_id", "seat_id"})})
public class BookedSeat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Long showId;
	private String seatId;
	private String tier;
	private Integer price;
	private String clerkUserId;
	private Long bookingId;

	public BookedSeat() {
		super();
	}

	public BookedSeat(Long id, Long showId, String seatId, String tier, Integer price, String clerkUserId,
			Long bookingId) {
		super();
		this.id = id;
		this.showId = showId;
		this.seatId = seatId;
		this.tier = tier;
		this.price = price;
		this.clerkUserId = clerkUserId;
		this.bookingId = bookingId;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getShowId() {
		return showId;
	}

	public void setShowId(Long showId) {
		this.showId = showId;
	}

	public String getSeatId() {
		return seatId;
	}

	public void setSeatId(String seatId) {
		this.seatId = seatId;
	}

	public String getTier() {
		return tier;
	}

	public void setTier(String tier) {
		this.tier = tier;
	}

	public Integer getPrice() {
		return price;
	}

	public void setPrice(Integer price) {
		this.price = price;
	}

	public String getClerkUserId() {
		return clerkUserId;
	}

	public void setClerkUserId(String clerkUserId) {
		this.clerkUserId = clerkUserId;
	}

	public Long getBookingId() {
		return bookingId;
	}

	public void setBookingId(Long bookingId) {
		this.bookingId = bookingId;
	}

	@Override
	public String toString() {
		return "BookedSeat [id=" + id + ", showId=" + showId + ", seatId=" + seatId + ", tier=" + tier + ", price="
				+ price + ", clerkUserId=" + clerkUserId + ", bookingId=" + bookingId + "]";
	}

}
