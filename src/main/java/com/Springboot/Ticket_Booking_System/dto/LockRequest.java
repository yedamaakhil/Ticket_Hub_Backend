package com.Springboot.Ticket_Booking_System.dto;

import java.util.List;

/**
 * Request body for POST /api/seats/lock
 *
 * Sending an empty seats list releases all locks held by sessionId for that
 * show.
 */
public class LockRequest {

	private Integer movieId;
	private String date; // "YYYY-MM-DD"
	private String time; // ISO time string matching show times
	/** Clerk userId when authenticated, or a UUID from sessionStorage otherwise */
	private String sessionId;
	/** Full current selection — replaces previous locks atomically */
	private List<String> seats;

	public Integer getMovieId() {
		return movieId;
	}

	public String getDate() {
		return date;
	}

	public String getTime() {
		return time;
	}

	public String getSessionId() {
		return sessionId;
	}

	public List<String> getSeats() {
		return seats;
	}

	public void setMovieId(Integer movieId) {
		this.movieId = movieId;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public void setSeats(List<String> seats) {
		this.seats = seats;
	}
}