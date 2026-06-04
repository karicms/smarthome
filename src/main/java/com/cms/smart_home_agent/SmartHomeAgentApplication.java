package com.cms.smart_home_agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartHomeAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartHomeAgentApplication.class, args);
	}

}
