package com.project.auto_complete_service.config;

import com.project.auto_complete_service.bktree.BKTree;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BKTreeConfig {

    @Bean
    public BKTree bkTree() {
        return new BKTree();
    }
}