package org.sid.auth_service.DTO;

import lombok.Data;

@Data
public class KeycloakProfileUpdateRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String companyName;
}
