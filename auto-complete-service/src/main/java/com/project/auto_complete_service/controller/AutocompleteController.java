package com.project.auto_complete_service.controller;

import com.project.auto_complete_service.kafka.producer.QueryProducer;
import com.project.auto_complete_service.service.AutocompleteService;
import com.project.auto_complete_service.service.PersonalizationService;
import com.project.auto_complete_service.service.TrendingService;
import com.project.auto_complete_service.scheduler.EmbeddingBackfillJob;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Autocomplete API", description = "Search suggestion APIs")
public class AutocompleteController {

    private final AutocompleteService service;
    private final QueryProducer producer;
    private final PersonalizationService personalizationService;
    private final TrendingService trendingService;
    private final EmbeddingBackfillJob embeddingBackfillJob;


    @Operation(summary = "Get personalized autocomplete suggestions")
    @GetMapping("/suggest")
    public ResponseEntity<List<String>> suggest(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit,
            // ← reads UUID from frontend — falls back to random if missing
            @RequestHeader(value = "X-Session-ID", required = false)
            String sessionId) {

       if (q == null || q.isBlank() || q.length() > 50) {
            return ResponseEntity.badRequest().build();
        }
        if (limit < 1 || limit > 10) {
            return ResponseEntity.badRequest().build();
        }

        // If frontend didn't send a session ID, generate one
        // (anonymous user — still works, just no personalization history yet)
        String resolvedSession = (sessionId != null && !sessionId.isBlank())
                ? sessionId
                : UUID.randomUUID().toString();

        String normalized = q.toLowerCase().trim();

        // Get personalized suggestions
        List<String> suggestions = service.getPersonalizedSuggestions(
                normalized, resolvedSession, limit
        );

        // Publish to Kafka with sessionId — async, non-blocking
        producer.publish(normalized, resolvedSession);

        return ResponseEntity.ok(suggestions);
    }


    // Add these two endpoints to AutocompleteController

    // GET /api/v1/user/history
// Shows what this user has searched (useful for debugging + demo)
    @GetMapping("/user/history")
    public ResponseEntity<List<String>> getUserHistory(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {

        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.ok(List.of());
        }

        return ResponseEntity.ok(
                personalizationService.getUserHistory(sessionId, 20)
        );
    }

    // DELETE /api/v1/user/history
// GDPR compliance — user can clear their own history
    @DeleteMapping("/user/history")
    public ResponseEntity<Void> clearHistory(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {

        if (sessionId != null && !sessionId.isBlank()) {
            personalizationService.clearUserHistory(sessionId);
        }

        return ResponseEntity.noContent().build();
    }


    @GetMapping("/suggest/rich")
    public ResponseEntity<List<SuggestionResponse>> suggestRich(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit,
            @RequestHeader(value = "X-Session-ID", required = false)
            String sessionId) {

        if (q == null || q.isBlank() || q.length() > 50) {
            return ResponseEntity.badRequest().build();
        }

        String normalized     = q.toLowerCase().trim();
        String resolvedSession = (sessionId != null && !sessionId.isBlank())
                ? sessionId : UUID.randomUUID().toString();

        // ✅ No fetchTrieOnly — source info comes from service
        List<AutocompleteService.SuggestionSource> results =
                service.getSuggestionsWithSource(normalized, resolvedSession, limit);

        List<SuggestionResponse> response = results.stream()
                .map(s -> new SuggestionResponse(
                        s.word(),
                        trendingService.isTrending(s.word(), 3.0),
                        s.source().startsWith("LLM")   // true for LLM or LLM_CACHE
                ))
                .toList();

        producer.publish(normalized, resolvedSession);

        return ResponseEntity.ok(response);
    }

    // Add to a new AdminController or existing controller
    @PostMapping("/admin/embeddings/backfill")
    public ResponseEntity<String> triggerBackfill() {
        embeddingBackfillJob.backfill();
        return ResponseEntity.ok("Backfill triggered");
    }

    public record SuggestionResponse(
            String word,
            boolean trending,
            boolean aiGenerated     // ← frontend can show "AI" badge
    ) {}

    private String resolveSession(String sessionId) {
        return (sessionId != null && !sessionId.isBlank())
                ? sessionId
                : UUID.randomUUID().toString();
    }

}