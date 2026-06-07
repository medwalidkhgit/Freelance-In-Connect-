package org.sid.auth_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"KEYCLOAK_URL=http://localhost:8080",
		"KEYCLOAK_REALM=b2b-platform",
		"KEYCLOAK_ADMIN_CLIENT_ID=auth-service",
		"KEYCLOAK_ADMIN_CLIENT_SECRET=test-admin-secret",
		"KEYCLOAK_USER_CLIENT_ID=b2b-app-client",
		"KEYCLOAK_USER_CLIENT_SECRET=test-user-secret",
		"INTERNAL_SERVICE_TOKEN=test-internal-token",
		"services.freelancer.url=http://localhost:8082",
		"services.company.url=http://localhost:8083"
})
class AuthServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
