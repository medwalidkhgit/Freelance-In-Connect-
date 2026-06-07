package org.sid.freelancer_service.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FreelancerRequest {
    private String keycloakUserId;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String summary;
    private String cvUrl;
    private String pfpUrl;
}