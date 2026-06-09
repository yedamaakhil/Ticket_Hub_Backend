package com.Springboot.Ticket_Booking_System.model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "shows")
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Core fields (already in your DB) ──
    private Integer   movieId;
    private LocalDate showDate;
    private String    showTime;

    // ── New fields: stored when AddShow.jsx creates a show ──
    // Run: ALTER TABLE shows ADD COLUMN theater_name VARCHAR(255);
    //      ALTER TABLE shows ADD COLUMN screen_name  VARCHAR(100);
    //      ALTER TABLE shows ADD COLUMN language     VARCHAR(50);
    //      ALTER TABLE shows ADD COLUMN total_seats  INT DEFAULT 100;
    //      ALTER TABLE shows ADD COLUMN ticket_price INT DEFAULT 0;
    // OR set spring.jpa.hibernate.ddl-auto=update and Spring will create them.
    private String  theaterName;
    private String  screenName;
    private String  language;
    private Integer totalSeats = 180;
    private Integer ticketPrice;

    public Show() { super(); }

    public Show(Long id, Integer movieId, LocalDate showDate, String showTime) {
        this.id      = id;
        this.movieId = movieId;
        this.showDate = showDate;
        this.showTime = showTime;
    }

    // ── Getters & Setters ──

    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }

    public Integer getMovieId()                 { return movieId; }
    public void setMovieId(Integer movieId)     { this.movieId = movieId; }

    public LocalDate getShowDate()              { return showDate; }
    public void setShowDate(LocalDate showDate) { this.showDate = showDate; }

    public String getShowTime()                 { return showTime; }
    public void setShowTime(String showTime)    { this.showTime = showTime; }

    public String getTheaterName()              { return theaterName; }
    public void setTheaterName(String v)        { this.theaterName = v; }

    public String getScreenName()               { return screenName; }
    public void setScreenName(String v)         { this.screenName = v; }

    public String getLanguage()                 { return language; }
    public void setLanguage(String v)           { this.language = v; }

    public Integer getTotalSeats()              { return totalSeats; }
    public void setTotalSeats(Integer v)        { this.totalSeats = v; }

    public Integer getTicketPrice()             { return ticketPrice; }
    public void setTicketPrice(Integer v)       { this.ticketPrice = v; }

    @Override
    public String toString() {
        return "Show [id=" + id + ", movieId=" + movieId + ", showDate=" + showDate
                + ", showTime=" + showTime + ", theaterName=" + theaterName
                + ", screenName=" + screenName + ", language=" + language
                + ", totalSeats=" + totalSeats + ", ticketPrice=" + ticketPrice + "]";
    }
}