package com.project.auto_complete_service.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Autocomplete Service API")
                        .version("1.0")
                        .description("Low-latency autocomplete system using Trie + Redis + Kafka"));
    }
}
