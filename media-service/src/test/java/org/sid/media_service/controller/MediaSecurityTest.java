package org.sid.media_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sid.media_service.dto.MediaUploadResponse;
import org.sid.media_service.dto.SignedUrlResponse;
import org.sid.media_service.service.MediaStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/b2b-platform",
        "azure.storage.connection-string=UseDevelopmentStorage=true"
})
@AutoConfigureMockMvc
class MediaSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MediaStorageService mediaStorageService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void actuatorHealthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void uploadProfilePictureWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(multipart("/api/media/profile-pictures").file(image()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadProfilePictureWithFreelancerRoleIsAllowed() throws Exception {
        when(jwtDecoder.decode("freelancer-token"))
                .thenReturn(jwtWithRole("freelancer-token", "freelancer-kc-1", "FREELANCER"));
        when(mediaStorageService.uploadProfilePicture(any(), eq("freelancer-kc-1")))
                .thenReturn(uploadResponse());

        mockMvc.perform(multipart("/api/media/profile-pictures")
                        .file(image())
                        .header("Authorization", "Bearer freelancer-token"))
                .andExpect(status().isOk());
    }

    @Test
    void uploadProfilePictureWithCompanyRoleIsForbidden() throws Exception {
        when(jwtDecoder.decode("company-token"))
                .thenReturn(jwtWithRole("company-token", "company-kc-1", "COMPANY"));

        mockMvc.perform(multipart("/api/media/profile-pictures")
                        .file(image())
                        .header("Authorization", "Bearer company-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadCompanyLogoWithCompanyRoleIsAllowed() throws Exception {
        when(jwtDecoder.decode("company-token"))
                .thenReturn(jwtWithRole("company-token", "company-kc-1", "COMPANY"));
        when(mediaStorageService.uploadCompanyLogo(any(), eq("company-kc-1")))
                .thenReturn(uploadResponse());

        mockMvc.perform(multipart("/api/media/company-logos")
                        .file(image())
                        .header("Authorization", "Bearer company-token"))
                .andExpect(status().isOk());
    }

    @Test
    void uploadCvWithCompanyRoleIsForbidden() throws Exception {
        when(jwtDecoder.decode("company-token"))
                .thenReturn(jwtWithRole("company-token", "company-kc-1", "COMPANY"));

        mockMvc.perform(multipart("/api/media/cvs")
                        .file(pdf())
                        .header("Authorization", "Bearer company-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void readUrlWithAuthenticatedPlatformRoleIsAllowed() throws Exception {
        when(jwtDecoder.decode("admin-token"))
                .thenReturn(jwtWithRole("admin-token", "admin-kc-1", "ADMIN"));
        when(mediaStorageService.createReadUrl("https://example.test/blob", null, null))
                .thenReturn(new SignedUrlResponse(
                        "profile-pictures",
                        "profile-pictures/admin-kc-1/blob.jpg",
                        "https://example.test/blob",
                        "https://example.test/blob?sas=1",
                        60
                ));

        mockMvc.perform(get("/api/media/read-url")
                        .param("url", "https://example.test/blob")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    private static MockMultipartFile image() {
        return new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3});
    }

    private static MockMultipartFile pdf() {
        return new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[]{1, 2, 3});
    }

    private static MediaUploadResponse uploadResponse() {
        return new MediaUploadResponse(
                "profile-pictures",
                "profile-pictures/user/blob.png",
                "https://example.test/blob.png",
                "https://example.test/blob.png?sas=1",
                "image/png",
                3
        );
    }

    private static Jwt jwtWithRole(String token, String subject, String role) {
        return Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", subject)
                .claim("realm_access", Map.of("roles", List.of(role)))
                .issuer("http://localhost:8080/realms/b2b-platform")
                .build();
    }
}
