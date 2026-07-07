package com.project.auto_complete_service.controller;

import com.project.auto_complete_service.service.TrendingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Trending API", description = "Real-time trending search terms")
public class TrendingController {

    private final TrendingService trendingService;

    // GET /api/v1/trending?limit=10
    @Operation(summary = "Get real-time trending search terms")
    @GetMapping("/trending")
    public ResponseEntity<List<TrendingService.TrendingTerm>> getTrending(
            @RequestParam(defaultValue = "10") int limit) {

        if (limit < 1 || limit > 50) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(trendingService.getTrending(limit));
    }

    // GET /api/v1/trending/check?term=kafka
    @Operation(summary = "Check if a specific term is currently trending")
    @GetMapping("/trending/check")
    public ResponseEntity<Boolean> isTrending(
            @RequestParam String term,
            @RequestParam(defaultValue = "5.0") double threshold) {

        return ResponseEntity.ok(
                trendingService.isTrending(term.toLowerCase().trim(), threshold)
        );
    }

    // GET /api/v1/trending/history?hoursAgo=3&limit=10
    @Operation(summary = "Get trending terms from N hours ago")
    @GetMapping("/trending/history")
    public ResponseEntity<List<TrendingService.TrendingTerm>> getTrendingHistory(
            @RequestParam(defaultValue = "1") int hoursAgo,
            @RequestParam(defaultValue = "10") int limit) {

        if (hoursAgo < 1 || hoursAgo > 24) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(
                trendingService.getTrendingByHour(hoursAgo, limit)
        );
    }
}