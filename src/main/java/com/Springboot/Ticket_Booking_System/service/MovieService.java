package com.Springboot.Ticket_Booking_System.service;

import com.Springboot.Ticket_Booking_System.dto.MovieRequest;
import com.Springboot.Ticket_Booking_System.model.Movie;
import com.Springboot.Ticket_Booking_System.repository.MovieRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MovieService {

	@Autowired
	private MovieRepository movieRepository;

	private final ObjectMapper mapper = new ObjectMapper();

	public List<Map<String, Object>> getAllMovies() {
		List<Map<String, Object>> result = new ArrayList<>();
		for (Movie m : movieRepository.findAll()) {
			result.add(toResponseMap(m));
		}
		return result;
	}

	public Map<String, Object> createMovie(MovieRequest req) {
		Movie movie = new Movie();
		applyRequestToMovie(req, movie);
		movie.setAddedBy("admin");
		movieRepository.save(movie);
		return toResponseMap(movie);
	}

	public Map<String, Object> updateMovie(Long id, MovieRequest req) {
		Movie movie = movieRepository.findById(id).orElse(null);
		if (movie == null)
			return null;
		applyRequestToMovie(req, movie);
		movieRepository.save(movie);
		return toResponseMap(movie);
	}

	public boolean deleteMovie(Long id) {
		if (!movieRepository.existsById(id))
			return false;
		movieRepository.deleteById(id);
		return true;
	}

	private void applyRequestToMovie(MovieRequest req, Movie movie) {
		movie.setTitle(req.getTitle());
		movie.setOverview(req.getOverview());
		movie.setPosterPath(req.getPoster_path());
		movie.setBackdropPath(req.getBackdrop_path());
		movie.setTrailerUrl(req.getTrailerUrl());
		movie.setReleaseDate(req.getRelease_date());
		movie.setOriginalLanguage(req.getOriginal_language());
		movie.setTagline(req.getTagline());
		movie.setVoteAverage(req.getVote_average());
		movie.setVoteCount(req.getVote_count());
		movie.setRuntime(req.getRuntime());
		movie.setTheater(req.getTheater());
		movie.setScreen(req.getScreen());
		try {
			movie.setGenresJson(mapper.writeValueAsString(req.getGenres()));
			movie.setCastsJson(mapper.writeValueAsString(req.getCasts()));
		} catch (Exception e) {
			throw new RuntimeException("Failed to serialize genres/casts", e);
		}
	}

	private Map<String, Object> toResponseMap(Movie m) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", m.getId());
		map.put("_id", String.valueOf(m.getId()));
		map.put("title", m.getTitle());
		map.put("overview", m.getOverview());
		map.put("poster_path", m.getPosterPath());
		map.put("backdrop_path", m.getBackdropPath());
		map.put("trailerUrl", m.getTrailerUrl());
		map.put("release_date", m.getReleaseDate());
		map.put("original_language", m.getOriginalLanguage());
		map.put("tagline", m.getTagline());
		map.put("vote_average", m.getVoteAverage());
		map.put("vote_count", m.getVoteCount());
		map.put("runtime", m.getRuntime());
		map.put("theater", m.getTheater());
		map.put("screen", m.getScreen());
		map.put("addedBy", m.getAddedBy());
		map.put("addedAt", m.getCreatedAt() != null ? m.getCreatedAt().toString() : null);
		map.put("updatedAt", m.getUpdatedAt() != null ? m.getUpdatedAt().toString() : null);
		try {
			map.put("genres", m.getGenresJson() != null ? mapper.readValue(m.getGenresJson(), List.class) : List.of());
			map.put("casts", m.getCastsJson() != null ? mapper.readValue(m.getCastsJson(), List.class) : List.of());
		} catch (Exception e) {
			map.put("genres", List.of());
			map.put("casts", List.of());
		}
		return map;
	}
}