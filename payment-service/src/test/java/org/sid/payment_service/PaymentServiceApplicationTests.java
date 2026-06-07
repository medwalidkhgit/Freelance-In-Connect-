package org.sid.payment_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:payment_context_test;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"stripe.secret-key=sk_test_context",
		"stripe.webhook-secret=whsec_test_context",
		"internal.service-token=test-internal-token",
		"spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/b2b-platform"
})
class PaymentServiceApplicationTests {

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void contextLoads() {
	}

}
