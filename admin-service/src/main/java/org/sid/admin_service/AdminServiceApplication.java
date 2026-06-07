package org.sid.admin_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.sid.admin_service.entities.Admin;
import org.sid.admin_service.repositories.AdminRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class AdminServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdminServiceApplication.class, args);
	}

	@Bean
	CommandLineRunner initAdmin(AdminRepository adminRepository) {
		return args -> {
			if (adminRepository.count() == 0) {
				Admin admin = Admin.builder()
					.username("admin")
					.email("admin@admin.com")
					.build();
				adminRepository.save(admin);
			}
		};
	}
}
