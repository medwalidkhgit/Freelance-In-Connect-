package org.sid.application_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class FreelancerProfileDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String fullname;
    private String summary;
    private List<String> skills;
    private List<String> experiences;
    private List<String> projects;

    public String displayName() {
        if (fullname != null && !fullname.isBlank()) {
            return fullname;
        }
        String first = firstName == null ? "" : firstName;
        String last = lastName == null ? "" : lastName;
        return (first + " " + last).trim();
    }
}
