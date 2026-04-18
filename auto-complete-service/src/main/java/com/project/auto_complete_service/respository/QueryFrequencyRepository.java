package com.project.auto_complete_service.respository;

import com.project.auto_complete_service.model.QueryFrequency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueryFrequencyRepository
        extends JpaRepository<QueryFrequency, String> {
}
