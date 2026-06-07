package org.sid.freelancer_service.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sid.freelancer_service.DTO.FreelancerRequest;
import org.sid.freelancer_service.DTO.FreelancerUpdateRequest;
import org.sid.freelancer_service.Entity.Freelancer;
import org.sid.freelancer_service.Repository.FreelancerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class FreelancerServiceBusinessRulesTest {

    @Mock
    private FreelancerRepository repository;

    @Mock
    private MissionServiceClient missionServiceClient;

    private FreelancerService freelancerService;

    @BeforeEach
    void setUp() {
        freelancerService = new FreelancerService(
                repository,
                missionServiceClient,
                new RestTemplate(),
                "http://payment-service:8088",
                false
        );
    }

    @Test
    void createFreelancerWithoutRequiredFieldsIsRejected() {
        FreelancerRequest request = validCreateRequest();
        request.setEmail(" ");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> freelancerService.createFreelancer(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any(Freelancer.class));
    }

    @Test
    void createFreelancerWithDuplicateEmailIsRejected() {
        FreelancerRequest request = validCreateRequest();
        when(repository.existsByEmail("freelancer@example.com")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> freelancerService.createFreelancer(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any(Freelancer.class));
    }

    @Test
    void suspendedProfileIsNotReturnedByKeycloakId() {
        when(repository.findByKeycloakUserIdAndSuspendedFalse("freelancer-1")).thenReturn(Optional.empty());

        assertTrue(freelancerService.getProfileByKeycloakId("freelancer-1").isEmpty());
    }

    @Test
    void suspendedProfileCannotBeUpdatedByOwner() {
        when(repository.findByKeycloakUserIdAndSuspendedFalse("freelancer-1")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> freelancerService.updateMyProfile("freelancer-1", validUpdateRequest())
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any(Freelancer.class));
    }

    @Test
    void updateMyProfileWithNullPayloadIsRejected() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> freelancerService.updateMyProfile("freelancer-1", null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(repository, never()).findByKeycloakUserIdAndSuspendedFalse("freelancer-1");
    }

    @Test
    void updateMyProfileForActiveFreelancerIsAllowed() {
        Freelancer existing = freelancer();
        FreelancerUpdateRequest request = validUpdateRequest();
        when(repository.findByKeycloakUserIdAndSuspendedFalse("freelancer-1")).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        Freelancer saved = freelancerService.updateMyProfile("freelancer-1", request);

        assertEquals("Updated", saved.getFirstName());
        verify(repository).save(existing);
    }

    private FreelancerRequest validCreateRequest() {
        FreelancerRequest request = new FreelancerRequest();
        request.setKeycloakUserId("freelancer-1");
        request.setEmail("freelancer@example.com");
        request.setFirstName("Freelancer");
        request.setLastName("One");
        return request;
    }

    private FreelancerUpdateRequest validUpdateRequest() {
        FreelancerUpdateRequest request = new FreelancerUpdateRequest();
        request.setFirstName("Updated");
        request.setLastName("One");
        return request;
    }

    private Freelancer freelancer() {
        Freelancer freelancer = new Freelancer();
        freelancer.setId(1L);
        freelancer.setKeycloakUserId("freelancer-1");
        freelancer.setEmail("freelancer@example.com");
        freelancer.setFirstName("Freelancer");
        freelancer.setLastName("One");
        freelancer.setSuspended(false);
        return freelancer;
    }
}
