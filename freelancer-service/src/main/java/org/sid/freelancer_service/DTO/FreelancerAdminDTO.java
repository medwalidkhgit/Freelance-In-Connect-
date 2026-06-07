package org.sid.freelancer_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FreelancerAdminDTO {
    private Long id;
    private String keycloakId;       // correspond à keycloakUserId
    private String fullname;         // firstName + " " + lastName
    private String email;
    private boolean suspended;       // correspond à isSuspended
}