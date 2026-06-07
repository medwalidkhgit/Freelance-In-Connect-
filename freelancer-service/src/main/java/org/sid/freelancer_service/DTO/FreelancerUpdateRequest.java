package org.sid.freelancer_service.DTO;

import lombok.Data;
import java.util.List;

@Data
public class FreelancerUpdateRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String summary;
    private String cvUrl;
    private String pfpUrl;
    private List<String> skills;
    private List<String> experiences;
    private List<String> projects;
}