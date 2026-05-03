package com.project.auto_complete_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AutoCompleteServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutoCompleteServiceApplication.class, args);
	}

}
