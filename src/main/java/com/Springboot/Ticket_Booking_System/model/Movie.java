package com.Springboot.Ticket_Booking_System.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "movies")
public class Movie {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private String title;
	private String poster_path;
	private String genres;
	private Integer runtime;
	private String release_date;
	private Double vote_average;

	public Movie() {
		super();
	}

	public Movie(Integer id, String title, String poster_path, String genres, Integer runtime, String release_date,
			Double vote_average) {
		super();
		this.id = id;
		this.title = title;
		this.poster_path = poster_path;
		this.genres = genres;
		this.runtime = runtime;
		this.release_date = release_date;
		this.vote_average = vote_average;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getPoster_path() {
		return poster_path;
	}

	public void setPoster_path(String poster_path) {
		this.poster_path = poster_path;
	}

	public String getGenres() {
		return genres;
	}

	public void setGenres(String genres) {
		this.genres = genres;
	}

	public Integer getRuntime() {
		return runtime;
	}

	public void setRuntime(Integer runtime) {
		this.runtime = runtime;
	}

	public String getRelease_date() {
		return release_date;
	}

	public void setRelease_date(String release_date) {
		this.release_date = release_date;
	}

	public Double getVote_average() {
		return vote_average;
	}

	public void setVote_average(Double vote_average) {
		this.vote_average = vote_average;
	}

	@Override
	public String toString() {
		return "Movie [id=" + id + ", title=" + title + ", poster_path=" + poster_path + ", genres=" + genres
				+ ", runtime=" + runtime + ", release_date=" + release_date + ", vote_average=" + vote_average + "]";
	}

}
