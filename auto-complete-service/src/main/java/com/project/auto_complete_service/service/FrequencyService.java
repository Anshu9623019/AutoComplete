package com.project.auto_complete_service.service;

import com.project.auto_complete_service.model.QueryFrequency;
import com.project.auto_complete_service.repository.QueryFrequencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FrequencyService {

    private final QueryFrequencyRepository repository;

    public void save(String query) {

        QueryFrequency entity = repository
                .findByWord(query)
                .orElse(
                        QueryFrequency.builder()
                                .word(query)
                                .frequency(0)
                                .build()
                );

        entity.setFrequency(entity.getFrequency() + 1);

        repository.save(entity);
    }
}