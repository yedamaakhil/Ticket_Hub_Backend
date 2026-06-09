package com.Springboot.Ticket_Booking_System.model;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.ElementCollection;
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
    private String clerkUserId;
    @CreationTimestamp
    private LocalDateTime createdAt;
    private String movieTitle;
    private String moviePosterPath;
    private String movieGenres;
    private Integer movieRuntime;
    private String movieLanguage;
    private String showDate;
    private String showTime;
    @ElementCollection
    private List<String> seats;
    private String bookingRef;
    private String transactionId;
    private String status;
    private Integer totalPrice;
    private String theaterName;
    private String screenName;

    public Booking() {
        super();
    }

    public Booking(Long id, Long showId, String clerkUserId, LocalDateTime createdAt, String movieTitle,
            String moviePosterPath, String movieGenres, Integer movieRuntime, String movieLanguage, String showDate,
            String showTime, List<String> seats, String bookingRef, String transactionId, String status,
            Integer totalPrice, String theaterName, String screenName) {
        super();
        this.id = id;
        this.showId = showId;
        this.clerkUserId = clerkUserId;
        this.createdAt = createdAt;
        this.movieTitle = movieTitle;
        this.moviePosterPath = moviePosterPath;
        this.movieGenres = movieGenres;
        this.movieRuntime = movieRuntime;
        this.movieLanguage = movieLanguage;
        this.showDate = showDate;
        this.showTime = showTime;
        this.seats = seats;
        this.bookingRef = bookingRef;
        this.transactionId = transactionId;
        this.status = status;
        this.totalPrice = totalPrice;
        this.theaterName = theaterName;
        this.screenName = screenName;
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

    public List<String> getSeats() { return seats; }
    public void setSeats(List<String> seats) { this.seats = seats; }

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

    @Override
    public String toString() {
        return "Booking [id=" + id + ", showId=" + showId + ", clerkUserId=" + clerkUserId + ", createdAt=" + createdAt
                + ", movieTitle=" + movieTitle + ", moviePosterPath=" + moviePosterPath + ", movieGenres=" + movieGenres
                + ", movieRuntime=" + movieRuntime + ", movieLanguage=" + movieLanguage + ", showDate=" + showDate
                + ", showTime=" + showTime + ", seats=" + seats + ", bookingRef=" + bookingRef + ", transactionId="
                + transactionId + ", status=" + status + ", totalPrice=" + totalPrice + ", theaterName=" + theaterName
                + ", screenName=" + screenName + "]";
    }
}