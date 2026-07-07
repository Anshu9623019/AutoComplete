package com.project.auto_complete_service.model;

import com.pgvector.PGvector;
import com.project.auto_complete_service.config.VectorConverter;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    // ✅ insertable=false, updatable=false — Hibernate never writes this column
    // Writes happen via native SQL updateEmbedding() which does the proper CAST
    // Reads work fine — PostgreSQL sends vector as string, we parse it
    @Column(columnDefinition = "vector(1536)", insertable = false, updatable = false)
    @Convert(converter = VectorConverter.class)
    private float[] embedding;

    public boolean hasEmbedding() {
        return embedding != null && embedding.length > 0;
    }
}