package com.project.auto_complete_service.service;


import com.project.auto_complete_service.model.QueryFrequency;
import com.project.auto_complete_service.repository.QueryFrequencyJdbcRepository;
import com.project.auto_complete_service.repository.QueryFrequencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QueryService {

    private final QueryFrequencyRepository jpaRepo;
    private final QueryFrequencyJdbcRepository jdbcRepo;

    public List<QueryFrequency> getAll() {
        return jpaRepo.findAll();
    }

    @Transactional
    public void bulkFlush(Map<String, Integer> snapshot) {
        jdbcRepo.bulkUpsert(snapshot);
    }
}
