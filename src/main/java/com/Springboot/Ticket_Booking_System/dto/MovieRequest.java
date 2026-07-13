package com.Springboot.Ticket_Booking_System.dto;

import java.util.List;
import java.util.Map;

public class MovieRequest {
	private String title;
	private String overview;
	private String poster_path;
	private String backdrop_path;
	private String trailerUrl;
	private String release_date;
	private String original_language;
	private String tagline;
	private Double vote_average;
	private Integer vote_count;
	private Integer runtime;
	private String theater;
	private String screen;
	private List<Map<String, Object>> genres;
	private List<Map<String, Object>> casts;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getOverview() {
		return overview;
	}

	public void setOverview(String overview) {
		this.overview = overview;
	}

	public String getPoster_path() {
		return poster_path;
	}

	public void setPoster_path(String poster_path) {
		this.poster_path = poster_path;
	}

	public String getBackdrop_path() {
		return backdrop_path;
	}

	public void setBackdrop_path(String backdrop_path) {
		this.backdrop_path = backdrop_path;
	}

	public String getTrailerUrl() {
		return trailerUrl;
	}

	public void setTrailerUrl(String trailerUrl) {
		this.trailerUrl = trailerUrl;
	}

	public String getRelease_date() {
		return release_date;
	}

	public void setRelease_date(String release_date) {
		this.release_date = release_date;
	}

	public String getOriginal_language() {
		return original_language;
	}

	public void setOriginal_language(String original_language) {
		this.original_language = original_language;
	}

	public String getTagline() {
		return tagline;
	}

	public void setTagline(String tagline) {
		this.tagline = tagline;
	}

	public Double getVote_average() {
		return vote_average;
	}

	public void setVote_average(Double vote_average) {
		this.vote_average = vote_average;
	}

	public Integer getVote_count() {
		return vote_count;
	}

	public void setVote_count(Integer vote_count) {
		this.vote_count = vote_count;
	}

	public Integer getRuntime() {
		return runtime;
	}

	public void setRuntime(Integer runtime) {
		this.runtime = runtime;
	}

	public String getTheater() {
		return theater;
	}

	public void setTheater(String theater) {
		this.theater = theater;
	}

	public String getScreen() {
		return screen;
	}

	public void setScreen(String screen) {
		this.screen = screen;
	}

	public List<Map<String, Object>> getGenres() {
		return genres;
	}

	public void setGenres(List<Map<String, Object>> genres) {
		this.genres = genres;
	}

	public List<Map<String, Object>> getCasts() {
		return casts;
	}

	public void setCasts(List<Map<String, Object>> casts) {
		this.casts = casts;
	}
}
