package com.airline.reservation.controller;

import com.airline.reservation.service.GeminiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for AI trip suggestions via Gemini.
 * Controller (GRASP): delegates entirely to GeminiService.
 * SRP: only HTTP routing concern.
 */
@RestController
@RequestMapping("/api/trips")
public class TripSuggestionController {

    private final GeminiService geminiService;

    public TripSuggestionController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    /** GET /api/trips/suggest?from=Mumbai&budget=15000&interests=beaches,food&duration=5 */
    @GetMapping("/suggest")
    public ResponseEntity<List<Map<String, Object>>> suggest(
            @RequestParam(defaultValue = "India") String from,
            @RequestParam(defaultValue = "15000") int budget,
            @RequestParam(defaultValue = "beaches, culture, food") String interests,
            @RequestParam(defaultValue = "5") int duration) {

        return ResponseEntity.ok(geminiService.suggestTrips(from, budget, interests, duration));
    }
}
