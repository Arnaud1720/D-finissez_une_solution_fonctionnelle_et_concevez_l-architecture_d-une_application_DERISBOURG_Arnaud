package com.arn.ycyw.your_car_your_way.controller;

import com.arn.ycyw.your_car_your_way.dto.MessageDto;
import com.arn.ycyw.your_car_your_way.security.UsersDetailsAdapter;
import com.arn.ycyw.your_car_your_way.services.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@Tag(name = "Messages", description = "API de gestion des messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    @Operation(summary = "Envoyer un message")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @Valid @RequestBody MessageDto messageDto,
            @AuthenticationPrincipal UsersDetailsAdapter principal) {

        Integer senderId = principal.getUser().getId();
        MessageDto sent = messageService.sendMessage(messageDto, senderId);

        URI location = URI.create("/api/messages/" + sent.getId());
        return ResponseEntity.created(location).body(Map.of(
                "message", "Message sent successfully",
                "data", sent
        ));
    }

    @GetMapping("/conversation/{conversationId}")
    @Operation(summary = "Recuperer les messages d'une conversation")
    public ResponseEntity<List<MessageDto>> getMessagesByConversation(
            @PathVariable Integer conversationId,
            @AuthenticationPrincipal UsersDetailsAdapter principal) {

        Integer currentUserId = principal.getUser().getId();
        List<MessageDto> messages = messageService.getMessagesByConversation(conversationId, currentUserId);
        return ResponseEntity.ok(messages);
    }

    @PatchMapping("/conversation/{conversationId}/read")
    @Operation(summary = "Marquer les messages d'une conversation comme lus")
    public ResponseEntity<Map<String, String>> markAsRead(
            @PathVariable Integer conversationId,
            @AuthenticationPrincipal UsersDetailsAdapter principal) {

        Integer currentUserId = principal.getUser().getId();
        messageService.markMessagesAsRead(conversationId, currentUserId);
        return ResponseEntity.ok(Map.of("message", "Messages marked as read"));
    }
}
