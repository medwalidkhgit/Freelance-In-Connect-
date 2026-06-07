package org.sid.admin_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"KEYCLOAK_URL=http://localhost:8080",
		"KEYCLOAK_REALM=b2b-platform",
		"SPRING_DATASOURCE_URL=jdbc:h2:mem:admin_context_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
		"SPRING_DATASOURCE_USERNAME=sa",
		"SPRING_DATASOURCE_PASSWORD=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"internal.service-token=test-internal-token",
		"services.company.url=http://localhost:8083",
		"services.freelancer.url=http://localhost:8082",
		"services.mission.url=http://localhost:8084"
})
class AdminServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
