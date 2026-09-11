package com.remindme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = { "com.target", "com.remindme" })
@EnableScheduling
public class RemindmeApplication {

	public static void main(String[] args) {
		SpringApplication.run(RemindmeApplication.class, args);
	}

}
