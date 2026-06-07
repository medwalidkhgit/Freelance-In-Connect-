package org.sid.messaging_service.repository;

import java.util.List;
import org.sid.messaging_service.domain.Message;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MessageRepository extends MongoRepository<Message, String> {

    List<Message> findByConversationIdOrderBySentAtAsc(String conversationId);
}
