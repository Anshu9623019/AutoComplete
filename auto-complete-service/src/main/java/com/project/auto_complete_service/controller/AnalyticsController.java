package com.project.auto_complete_service.controller;

import com.project.auto_complete_service.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics API", description = "Search analytics and insights")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // GET /api/v1/analytics
    @Operation(summary = "Full analytics snapshot")
    @GetMapping
    public ResponseEntity<AnalyticsService.AnalyticsSnapshot> getSnapshot() {
        return ResponseEntity.ok(analyticsService.getSnapshot());
    }

    // GET /api/v1/analytics/top-terms?limit=10
    @Operation(summary = "Top searched terms")
    @GetMapping("/top-terms")
    public ResponseEntity<List<AnalyticsService.TermCount>> getTopTerms(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.getTopTerms(limit));
    }

    // GET /api/v1/analytics/volume?hours=24
    @Operation(summary = "Search volume by hour")
    @GetMapping("/volume")
    public ResponseEntity<List<AnalyticsService.HourlyVolume>> getVolume(
            @RequestParam(defaultValue = "24") int hours) {

        if (hours < 1 || hours > 48) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(
                analyticsService.getSearchVolumeByHour(hours));
    }
}
