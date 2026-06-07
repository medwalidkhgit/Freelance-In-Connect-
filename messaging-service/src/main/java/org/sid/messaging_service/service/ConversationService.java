package org.sid.messaging_service.service;

import java.util.List;
import java.util.Optional;
import org.sid.messaging_service.domain.Conversation;
import org.sid.messaging_service.repository.ConversationRepository;
import org.sid.messaging_service.security.MessagingUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public Conversation createOrGet(Long missionId, Long companyId, Long freelancerId,
                                    String companyKeycloakId, String freelancerKeycloakId,
                                    MessagingUser user) {
        assertRequestedBusinessIdMatchesToken(companyId, freelancerId, user);

        String effectiveCompanyKeycloakId = effectiveCompanyKeycloakId(companyKeycloakId, user);
        String effectiveFreelancerKeycloakId = effectiveFreelancerKeycloakId(freelancerKeycloakId, user);

        assertCanAccess(companyId, freelancerId, effectiveCompanyKeycloakId, effectiveFreelancerKeycloakId, user);

        Optional<Conversation> existing = conversationRepository.findByMissionIdAndCompanyIdAndFreelancerId(
            missionId, companyId, freelancerId
        );
        if (existing.isPresent()) {
            Conversation conversation = existing.get();
            boolean updated = fillMissingKeycloakIds(conversation, effectiveCompanyKeycloakId, effectiveFreelancerKeycloakId);
            return updated ? conversationRepository.save(conversation) : conversation;
        }

        Conversation conversation = new Conversation();
        conversation.setMissionId(missionId);
        conversation.setCompanyId(companyId);
        conversation.setFreelancerId(freelancerId);
        conversation.setCompanyKeycloakId(effectiveCompanyKeycloakId);
        conversation.setFreelancerKeycloakId(effectiveFreelancerKeycloakId);
        return conversationRepository.save(conversation);
    }

    public List<Conversation> findVisibleFor(MessagingUser user) {
        if (user.admin()) {
            return conversationRepository.findAll();
        }
        if (user.company()) {
            requireKeycloakId(user);
            List<Conversation> conversations = conversationRepository.findByCompanyKeycloakId(user.keycloakId());
            if (!conversations.isEmpty() || user.companyId() == null) {
                return conversations;
            }
            return conversationRepository.findByCompanyId(user.companyId());
        }
        if (user.freelancer()) {
            requireKeycloakId(user);
            List<Conversation> conversations = conversationRepository.findByFreelancerKeycloakId(user.keycloakId());
            if (!conversations.isEmpty() || user.freelancerId() == null) {
                return conversations;
            }
            return conversationRepository.findByFreelancerId(user.freelancerId());
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role not allowed for conversations");
    }

    public Conversation getVisibleConversation(String conversationId, MessagingUser user) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        assertCanAccess(conversation, user);
        return conversation;
    }

    public void assertCanAccess(Conversation conversation, MessagingUser user) {
        assertCanAccess(
                conversation.getCompanyId(),
                conversation.getFreelancerId(),
                conversation.getCompanyKeycloakId(),
                conversation.getFreelancerKeycloakId(),
                user
        );
    }

    private void assertCanAccess(Long companyId, Long freelancerId,
                                 String companyKeycloakId, String freelancerKeycloakId,
                                 MessagingUser user) {
        if (user.admin()) {
            return;
        }
        requireKeycloakId(user);

        if (user.company()) {
            if (user.keycloakId().equals(companyKeycloakId)) {
                return;
            }
            if (isBlank(companyKeycloakId) && user.companyId() != null && user.companyId().equals(companyId)) {
                return;
            }
        }
        if (user.freelancer()) {
            if (user.keycloakId().equals(freelancerKeycloakId)) {
                return;
            }
            if (isBlank(freelancerKeycloakId) && user.freelancerId() != null && user.freelancerId().equals(freelancerId)) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conversation access denied");
    }

    private String effectiveCompanyKeycloakId(String requestedCompanyKeycloakId, MessagingUser user) {
        if (user.company()) {
            requireKeycloakId(user);
            return user.keycloakId();
        }
        return requestedCompanyKeycloakId;
    }

    private void assertRequestedBusinessIdMatchesToken(Long companyId, Long freelancerId, MessagingUser user) {
        if (user.company() && user.companyId() != null && !user.companyId().equals(companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conversation access denied");
        }
        if (user.freelancer() && user.freelancerId() != null && !user.freelancerId().equals(freelancerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conversation access denied");
        }
    }

    private String effectiveFreelancerKeycloakId(String requestedFreelancerKeycloakId, MessagingUser user) {
        if (user.freelancer()) {
            requireKeycloakId(user);
            return user.keycloakId();
        }
        return requestedFreelancerKeycloakId;
    }

    private boolean fillMissingKeycloakIds(Conversation conversation, String companyKeycloakId, String freelancerKeycloakId) {
        boolean updated = false;
        if (isBlank(conversation.getCompanyKeycloakId()) && !isBlank(companyKeycloakId)) {
            conversation.setCompanyKeycloakId(companyKeycloakId);
            updated = true;
        }
        if (isBlank(conversation.getFreelancerKeycloakId()) && !isBlank(freelancerKeycloakId)) {
            conversation.setFreelancerKeycloakId(freelancerKeycloakId);
            updated = true;
        }
        return updated;
    }

    private void requireKeycloakId(MessagingUser user) {
        if (!user.hasKeycloakId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Keycloak subject is required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
