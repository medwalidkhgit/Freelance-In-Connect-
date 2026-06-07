package org.sid.freelancer_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakProfileUpdateRequest {
    private String firstName;
    private String lastName;
    private String phone;
}
