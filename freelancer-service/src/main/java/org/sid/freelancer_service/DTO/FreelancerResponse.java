package org.sid.freelancer_service.DTO;

import lombok.Data;

@Data
public class FreelancerResponse {
    private Long id;
    private String keycloakUserId;
    private String email;
    private String firstName;
    private String lastName;
    private String stripeAccountId;
    private String stripeOnboardingUrl;
}
