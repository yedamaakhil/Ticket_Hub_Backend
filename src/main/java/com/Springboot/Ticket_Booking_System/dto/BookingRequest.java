package com.Springboot.Ticket_Booking_System.dto;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAlias;

public class BookingRequest {
	// ── Show fields ──
	private Integer movieId;
	private String showDate; // "2026-04-25"
	private String showTime; // "14:30:00"
	private List<String> seats; // ["A1", "B3", "K5"]
	private Map<String, Integer> seatPrices; // {"A1": 150, "B3": 300}
	private Integer totalPrice;
	private String paymentMethod;
	// ── Razorpay payment ID (from frontend after Razorpay verification) ──
	private String razorpayPaymentId; // "pay_XXXXXX"
	// ── Movie metadata (stored directly on Booking entity) ──
	private String movieTitle;
	private String moviePosterPath;
	private String movieGenres;
	private Integer movieRuntime;
	private String movieLanguage;
	// ── Venue ──
	private String theaterName;
	private String screenName;
	// ── User email for confirmation email ──
	@JsonAlias({ "user_email", "email" })
	private String userEmail;
	// ── Seat lock session id (Clerk userId or UUID from sessionStorage) ──
	private String sessionId;

	// ─────────────────────────────────────────────
	// Constructor
	// ─────────────────────────────────────────────
	public BookingRequest() {
	}

	// ─────────────────────────────────────────────
	// Getters & Setters
	// ─────────────────────────────────────────────
	public Integer getMovieId() {
		return movieId;
	}

	public void setMovieId(Integer movieId) {
		this.movieId = movieId;
	}

	public String getShowDate() {
		return showDate;
	}

	public void setShowDate(String showDate) {
		this.showDate = showDate;
	}

	public String getShowTime() {
		return showTime;
	}

	public void setShowTime(String showTime) {
		this.showTime = showTime;
	}

	public List<String> getSeats() {
		return seats;
	}

	public void setSeats(List<String> seats) {
		this.seats = seats;
	}

	public Map<String, Integer> getSeatPrices() {
		return seatPrices;
	}

	public void setSeatPrices(Map<String, Integer> p) {
		this.seatPrices = p;
	}

	public Integer getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(Integer totalPrice) {
		this.totalPrice = totalPrice;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	// ── Razorpay ──
	public String getRazorpayPaymentId() {
		return razorpayPaymentId;
	}

	public void setRazorpayPaymentId(String razorpayPaymentId) {
		this.razorpayPaymentId = razorpayPaymentId;
	}

	// ── Movie metadata ──
	public String getMovieTitle() {
		return movieTitle;
	}

	public void setMovieTitle(String movieTitle) {
		this.movieTitle = movieTitle;
	}

	public String getMoviePosterPath() {
		return moviePosterPath;
	}

	public void setMoviePosterPath(String path) {
		this.moviePosterPath = path;
	}

	public String getMovieGenres() {
		return movieGenres;
	}

	public void setMovieGenres(String movieGenres) {
		this.movieGenres = movieGenres;
	}

	public Integer getMovieRuntime() {
		return movieRuntime;
	}

	public void setMovieRuntime(Integer movieRuntime) {
		this.movieRuntime = movieRuntime;
	}

	public String getMovieLanguage() {
		return movieLanguage;
	}

	public void setMovieLanguage(String movieLanguage) {
		this.movieLanguage = movieLanguage;
	}

	// ── Venue ──
	public String getTheaterName() {
		return theaterName;
	}

	public void setTheaterName(String theaterName) {
		this.theaterName = theaterName;
	}

	public String getScreenName() {
		return screenName;
	}

	public void setScreenName(String screenName) {
		this.screenName = screenName;
	}

	// ── Email ──
	public String getUserEmail() {
		return userEmail;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	// ── Session id ──
	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	@Override
	public String toString() {
		return "BookingRequest [movieId=" + movieId + ", showDate=" + showDate + ", showTime=" + showTime + ", seats="
				+ seats + ", totalPrice=" + totalPrice + ", paymentMethod=" + paymentMethod + ", razorpayPaymentId="
				+ razorpayPaymentId + ", userEmail=" + userEmail + ", theaterName=" + theaterName + ", sessionId="
				+ sessionId + "]";
	}
}