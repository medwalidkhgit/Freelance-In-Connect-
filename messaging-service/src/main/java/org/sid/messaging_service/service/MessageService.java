package org.sid.messaging_service.service;

import java.util.List;
import org.sid.messaging_service.domain.Conversation;
import org.sid.messaging_service.domain.Message;
import org.sid.messaging_service.repository.MessageRepository;
import org.sid.messaging_service.security.MessagingUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MessageService {

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public Message saveMessage(Conversation conversation, MessagingUser user, String content) {
        if (!user.hasKeycloakId() || user.senderRole().equals("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only conversation participants can send messages");
        }

        Message message = new Message();
        message.setConversationId(conversation.getId());
        message.setSenderId(user.senderId());
        message.setSenderKeycloakId(user.keycloakId());
        message.setSenderRole(user.senderRole());
        message.setContent(content);
        return messageRepository.save(message);
    }

    public List<Message> getHistory(String conversationId) {
        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId);
    }
}
