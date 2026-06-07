package org.sid.company_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:company_context_test;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"KEYCLOAK_URL=http://localhost:8080",
		"KEYCLOAK_REALM=b2b-platform",
		"internal.service-token=test-internal-token",
		"services.mission.url=http://localhost:8084"
})
class CompanyServiceApplicationTests {

	@MockBean
	private JwtDecoder jwtDecoder;

	@Test
	void contextLoads() {
	}

}
