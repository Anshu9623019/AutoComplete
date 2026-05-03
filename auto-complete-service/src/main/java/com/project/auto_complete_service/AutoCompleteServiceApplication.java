package com.project.auto_complete_service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class AutoCompleteServiceApplication {

	@Autowired
	RedisTemplate redisTemplate;

	public static void main(String[] args) {
		SpringApplication.run(AutoCompleteServiceApplication.class, args);
	}


	@Bean
	public CommandLineRunner testRedis(RedisConnectionFactory factory) {
		return args -> {
			System.out.println("REDIS FACTORY = " + factory.getConnection().getClientName());
			System.out.println("REDIS SERVER = " + factory.getConnection().serverCommands().info());
		};
	}
	@Autowired
	private RedisConnectionFactory factory;

	@PostConstruct
	public void checkRedis() {
		System.out.println(factory.getConnection().ping());
	}
	@PostConstruct
	public void test() {
		redisTemplate.opsForValue().set("hello", "world");
	}
}
