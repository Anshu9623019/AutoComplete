package com.project.auto_complete_service.service;


import com.project.auto_complete_service.model.SuggestionEntry;
import com.project.auto_complete_service.tries.AutocompleteTrie;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AutocompleteService {

    private final AutocompleteTrie trie;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String PREFIX = "ac:";
    private static final long TTL = 300;

    public List<String> getSuggestions(String prefix, int limit) {

        String key = PREFIX + prefix;

        // 🔥 L1 Cache (Redis)
        Set<String> cached = redisTemplate.opsForZSet()
                .reverseRange(key, 0, limit - 1);

        if (cached != null && !cached.isEmpty()) {
            return new ArrayList<>(cached);
        }

        // 🔥 L2 Cache (Trie)
        List<SuggestionEntry> entries = trie.search(prefix);

        List<String> result = entries.stream()
                .map(SuggestionEntry::word)
                .limit(limit)
                .toList();

        // Cache result
        if (!entries.isEmpty()) {
            entries.forEach(e ->
                    redisTemplate.opsForZSet()
                            .add(key, e.word(), e.frequency())
            );

            redisTemplate.expire(key, Duration.ofSeconds(TTL));
        }

        return result;
    }
}
