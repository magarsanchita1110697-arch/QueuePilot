package com.queuepilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class QueuepilotBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(QueuepilotBackendApplication.class, args);
	}

}
