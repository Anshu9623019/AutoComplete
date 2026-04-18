package com.project.auto_complete_service.controller;

import com.project.auto_complete_service.service.AutocompleteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Autocomplete API", description = "Search suggestion APIs")
public class AutocompleteController {

    private final AutocompleteService service;

    @Operation(summary = "Get autocomplete suggestions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @GetMapping("/suggest")
    public ResponseEntity<List<String>> suggest(
            @Parameter(description = "Search prefix") @RequestParam String q,
            @Parameter(description = "Max results") @RequestParam(defaultValue = "5") int limit) {

        if (q == null || q.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(
                service.getSuggestions(q.toLowerCase().trim(), limit)
        );
    }
}