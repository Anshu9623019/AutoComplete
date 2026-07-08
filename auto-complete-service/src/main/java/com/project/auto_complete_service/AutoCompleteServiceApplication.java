package com.project.auto_complete_service;

import com.project.auto_complete_service.config.RenderKafkaCertInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class AutoCompleteServiceApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(AutoCompleteServiceApplication.class);

		// Add the initializer right here so it executes before any listeners boot up
		application.addListeners(new RenderKafkaCertInitializer());

		application.run(args);
	}
}