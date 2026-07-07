package com.project.auto_complete_service.model;

public record SearchEvent(
        String query,
        String sessionId    // ← added — the UUID from frontend
) {}

