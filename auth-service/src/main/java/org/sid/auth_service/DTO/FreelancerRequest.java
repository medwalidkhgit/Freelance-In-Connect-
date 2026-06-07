package org.sid.auth_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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