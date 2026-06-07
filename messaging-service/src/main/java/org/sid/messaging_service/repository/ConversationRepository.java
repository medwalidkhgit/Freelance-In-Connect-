package org.sid.messaging_service.repository;

import java.util.Optional;
import java.util.List;
import org.sid.messaging_service.domain.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ConversationRepository extends MongoRepository<Conversation, String> {

    Optional<Conversation> findByMissionIdAndCompanyIdAndFreelancerId(Long missionId, Long companyId, Long freelancerId);

    List<Conversation> findByCompanyId(Long companyId);

    List<Conversation> findByFreelancerId(Long freelancerId);

    List<Conversation> findByCompanyKeycloakId(String companyKeycloakId);

    List<Conversation> findByFreelancerKeycloakId(String freelancerKeycloakId);
}
