package com.project.auto_complete_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "query_frequency")
@Data
public class QueryFrequency {

    @Id
    private String word;
    private int frequency;
}
