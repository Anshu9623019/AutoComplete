package com.project.auto_complete_service.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class QueryFrequencyJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public void bulkUpsert(Map<String, Integer> snapshot) {

        String sql = """
            INSERT INTO query_frequency(word, frequency)
            VALUES (?, ?)
            ON CONFLICT(word)
            DO UPDATE SET frequency =
                query_frequency.frequency + EXCLUDED.frequency
            """;

        List<Map.Entry<String, Integer>> entries =
                new ArrayList<>(snapshot.entrySet());

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i)
                    throws SQLException {

                Map.Entry<String, Integer> entry = entries.get(i);

                ps.setString(1, entry.getKey());
                ps.setInt(2, entry.getValue());
            }

            @Override
            public int getBatchSize() {
                return entries.size();
            }
        });
    }
}