package com.Springboot.Ticket_Booking_System.controller;

import com.Springboot.Ticket_Booking_System.dto.MovieRequest;
import com.Springboot.Ticket_Booking_System.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

	@Autowired
	private MovieService movieService;

	// GET — every user/device fetches the same list from the DB
	@GetMapping
	public ResponseEntity<List<Map<String, Object>>> getAllMovies() {
		return ResponseEntity.ok(movieService.getAllMovies());
	}

	// POST — admin adds a movie
	@PostMapping
	public ResponseEntity<Map<String, Object>> createMovie(@RequestBody MovieRequest req) {
		return ResponseEntity.ok(movieService.createMovie(req));
	}

	// PUT — admin edits a movie
	@PutMapping("/{id}")
	public ResponseEntity<Map<String, Object>> updateMovie(@PathVariable Long id, @RequestBody MovieRequest req) {
		Map<String, Object> updated = movieService.updateMovie(id, req);
		if (updated == null)
			return ResponseEntity.notFound().build();
		return ResponseEntity.ok(updated);
	}

	// DELETE — admin removes a movie
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
		boolean deleted = movieService.deleteMovie(id);
		if (!deleted)
			return ResponseEntity.notFound().build();
		return ResponseEntity.noContent().build();
	}
}