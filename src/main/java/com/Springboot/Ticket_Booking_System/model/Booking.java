package com.Springboot.Ticket_Booking_System.model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long showId;

    @Column(length = 255)
    private String clerkUserId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(length = 500)
    private String movieTitle;

    @Column(columnDefinition = "TEXT")
    private String moviePosterPath;

    @Column(length = 500)
    private String movieGenres;
    private Integer movieRuntime;

    @Column(length = 50)
    private String movieLanguage;

    @Column(length = 20)
    private String showDate;

    @Column(length = 50)
    private String showTime;

    @Column(name = "seats_csv", columnDefinition = "TEXT")
    private String seatsCsv;

    @Column(length = 50)
    private String bookingRef;

    @Column(length = 100)
    private String transactionId;

    @Column(length = 30)
    private String status;
    private Integer totalPrice;

    @Column(length = 255)
    private String theaterName;

    @Column(length = 100)
    private String screenName;

    public Booking() {
        super();
    }

    public List<String> getSeats() {
        if (seatsCsv == null || seatsCsv.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.asList(seatsCsv.split(","));
    }

    public void setSeats(List<String> seats) {
        if (seats == null || seats.isEmpty()) {
            this.seatsCsv = null;
            return;
        }
        this.seatsCsv = String.join(",", seats);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getShowId() { return showId; }
    public void setShowId(Long showId) { this.showId = showId; }

    public String getClerkUserId() { return clerkUserId; }
    public void setClerkUserId(String clerkUserId) { this.clerkUserId = clerkUserId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }

    public String getMoviePosterPath() { return moviePosterPath; }
    public void setMoviePosterPath(String moviePosterPath) { this.moviePosterPath = moviePosterPath; }

    public String getMovieGenres() { return movieGenres; }
    public void setMovieGenres(String movieGenres) { this.movieGenres = movieGenres; }

    public Integer getMovieRuntime() { return movieRuntime; }
    public void setMovieRuntime(Integer movieRuntime) { this.movieRuntime = movieRuntime; }

    public String getMovieLanguage() { return movieLanguage; }
    public void setMovieLanguage(String movieLanguage) { this.movieLanguage = movieLanguage; }

    public String getShowDate() { return showDate; }
    public void setShowDate(String showDate) { this.showDate = showDate; }

    public String getShowTime() { return showTime; }
    public void setShowTime(String showTime) { this.showTime = showTime; }

    public String getBookingRef() { return bookingRef; }
    public void setBookingRef(String bookingRef) { this.bookingRef = bookingRef; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Integer totalPrice) { this.totalPrice = totalPrice; }

    public String getTheaterName() { return theaterName; }
    public void setTheaterName(String theaterName) { this.theaterName = theaterName; }

    public String getScreenName() { return screenName; }
    public void setScreenName(String screenName) { this.screenName = screenName; }
}
