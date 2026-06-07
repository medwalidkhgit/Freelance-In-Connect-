package org.sid.application_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ApplicationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApplicationServiceApplication.class, args);
	}

}
