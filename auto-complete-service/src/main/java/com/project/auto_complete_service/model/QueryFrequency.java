package com.project.auto_complete_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "query_frequency")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryFrequency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String word;

    @Column(nullable = false)
    private Integer frequency;
}