package com.remindme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = { "com.target", "com.remindme" })
public class RemindmeApplication {

	public static void main(String[] args) {
		SpringApplication.run(RemindmeApplication.class, args);
	}

}
