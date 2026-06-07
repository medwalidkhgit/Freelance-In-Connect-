package org.sid.auth_service.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FreelancerRegisterRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank @Email
    private String email;

    @Pattern(regexp = "^[0-9+\\-() ]+$", message = "Invalid phone number")
    private String phone;

    private String summary;

    @NotBlank
    @Size(min = 8, message = "Le mot de passe doit contenir au minimum 8 caractères")
    private String password;

    private String cvUrl;

    private String pfpUrl;

}