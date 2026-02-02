package com.arn.ycyw.your_car_your_way.controller;

import com.arn.ycyw.your_car_your_way.dto.MessageDto;
import com.arn.ycyw.your_car_your_way.security.UsersDetailsAdapter;
import com.arn.ycyw.your_car_your_way.services.MessageService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * Contrôleur WebSocket/STOMP pour le chat en temps réel.
 * Conforme ADD V2 §6.5 et BR-SUP-02.
 *
 * Flow :
 * 1. Client envoie un message via STOMP vers /app/chat/{conversationId}
 * 2. Le message est persisté en base via MessageService
 * 3. Le message est broadcasté vers /topic/conversation/{conversationId}
 * 4. Tous les abonnés au topic reçoivent le message en temps réel (< 2s - CC-17)
 */
@Controller
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;

    public ChatWebSocketController(SimpMessagingTemplate messagingTemplate,
                                    MessageService messageService) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
    }

    /**
     * Réception d'un message STOMP envoyé par le client.
     * Destination : /app/chat/{conversationId}
     * Broadcast vers : /topic/conversation/{conversationId}
     */
    @MessageMapping("/chat/{conversationId}")
    public void sendMessage(@DestinationVariable Integer conversationId,
                            @Payload MessageDto messageDto,
                            Principal principal) {

        // Extraire l'ID de l'utilisateur depuis le principal JWT
        Integer senderId = extractUserId(principal);

        // Forcer le conversationId depuis l'URL (sécurité)
        messageDto.setConversationId(conversationId);

        // Persister le message en base de données
        MessageDto savedMessage = messageService.sendMessage(messageDto, senderId);

        // Broadcaster le message à tous les abonnés du topic de la conversation
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId,
                savedMessage
        );
    }

    /**
     * Extraction de l'ID utilisateur depuis le Principal (JWT via WebSocket)
     */
    private Integer extractUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            Object details = auth.getPrincipal();
            if (details instanceof UsersDetailsAdapter adapter) {
                return adapter.getUser().getId();
            }
        }
        throw new IllegalStateException("Unable to extract user ID from WebSocket principal");
    }
}
