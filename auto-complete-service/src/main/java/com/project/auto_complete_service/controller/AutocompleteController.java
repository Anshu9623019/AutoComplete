package com.project.auto_complete_service.controller;


import com.project.auto_complete_service.kafka.producer.QueryProducer;
import com.project.auto_complete_service.service.AutocompleteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Autocomplete API", description = "Search suggestion APIs")
public class AutocompleteController {

    private final AutocompleteService service;
    private final QueryProducer producer;          // ← inject producer

    @Operation(summary = "Get autocomplete suggestions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @GetMapping("/suggest")
    public ResponseEntity<List<String>> suggest(
            @Parameter(description = "Search prefix") @RequestParam String q,
            @Parameter(description = "Max results (1-10)") @RequestParam(defaultValue = "5") int limit) {

        // Validate
        if (q == null || q.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (q.length() > 50) {                     // ← guard against huge prefixes
            return ResponseEntity.badRequest().build();
        }
        if (limit < 1 || limit > 10) {             // ← guard against limit abuse
            return ResponseEntity.badRequest().build();
        }

        String normalized = q.toLowerCase().trim();

        List<String> suggestions = service.getSuggestions(normalized, limit);

        // Publish to Kafka AFTER successful fetch — non-blocking
        producer.publish(normalized);              // ← the missing call

        return ResponseEntity.ok(suggestions);
    }
}