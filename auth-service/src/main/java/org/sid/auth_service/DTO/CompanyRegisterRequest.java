package org.sid.auth_service.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompanyRegisterRequest {

    @NotBlank @Email
    private String email;

    @NotBlank
    @Size(min = 8, message = "Le mot de passe doit contenir au minimum 8 caractères")
    private String password;

    @NotBlank
    private String companyName;

    @NotBlank
    @Pattern(regexp = "\\d{14}", message = "SIRET must be 14 digits")
    private String siret;

    @NotBlank
    private String contactFirstName;

    @NotBlank
    private String contactLastName;

    private String companyAddress;
    private String companyPhone;
    private String domaine;
    private String pfpUrl;
}
