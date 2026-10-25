package com.rohan.harbored;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HarboredApplication {

	public static void main(String[] args) {
		SpringApplication.run(HarboredApplication.class, args);
	}
}