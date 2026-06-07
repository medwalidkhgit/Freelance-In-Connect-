package org.sid.freelancer_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FreelancerProfileDTO {
        private Long id;
        private String keycloakUserId;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String fullname;
        private String summary;
        private String cvUrl;
        private String pfpUrl;
        private List<String> skills;
        private List<String> experiences;
        private List<String> projects;
}
