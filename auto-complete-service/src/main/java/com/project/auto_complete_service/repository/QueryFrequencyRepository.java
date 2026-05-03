package com.project.auto_complete_service.repository;

import com.project.auto_complete_service.model.QueryFrequency;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;


@Repository
public interface QueryFrequencyRepository extends JpaRepository<QueryFrequency, Long> {

}
