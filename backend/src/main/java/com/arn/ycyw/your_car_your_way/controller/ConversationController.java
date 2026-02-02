package com.arn.ycyw.your_car_your_way.controller;

import com.arn.ycyw.your_car_your_way.dto.ConversationDto;
import com.arn.ycyw.your_car_your_way.security.UsersDetailsAdapter;
import com.arn.ycyw.your_car_your_way.services.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Conversations", description = "API de gestion des conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    @Operation(summary = "Creer une nouvelle conversation")
    public ResponseEntity<Map<String, Object>> createConversation(
            @Valid @RequestBody ConversationDto conversationDto,
            @AuthenticationPrincipal UsersDetailsAdapter principal) {

        Integer customerId = principal.getUser().getId();
        ConversationDto created = conversationService.createConversation(conversationDto, customerId);

        URI location = URI.create("/api/conversations/" + created.getId());
        return ResponseEntity.created(location).body(Map.of(
                "message", "Conversation created successfully",
                "conversation", created
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer une conversation par ID")
    public ResponseEntity<ConversationDto> getConversationById(
            @PathVariable Integer id,
            @AuthenticationPrincipal UsersDetailsAdapter principal) {

        Integer currentUserId = principal.getUser().getId();
        ConversationDto conversation = conversationService.getConversationById(id, currentUserId);
        return ResponseEntity.ok(conversation);
    }

    @GetMapping("/my")
    @Operation(summary = "Recuperer mes conversations")
    public ResponseEntity<List<ConversationDto>> getMyConversations(
            @AuthenticationPrincipal UsersDetailsAdapter principal) {

        Integer userId = principal.getUser().getId();
        List<ConversationDto> conversations = conversationService.getMyConversations(userId);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/unassigned")
    @Operation(summary = "Recuperer les conversations non assignees (employees uniquement)")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<List<ConversationDto>> getUnassignedConversations() {
        List<ConversationDto> conversations = conversationService.getUnassignedConversations();
        return ResponseEntity.ok(conversations);
    }

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Assigner une conversation a un employe")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<ConversationDto> assignConversation(
            @PathVariable Integer id,
            @AuthenticationPrincipal UsersDetailsAdapter principal) {

        Integer employeeId = principal.getUser().getId();
        ConversationDto conversation = conversationService.assignEmployee(id, employeeId);
        return ResponseEntity.ok(conversation);
    }

    @PatchMapping("/{id}/close")
    @Operation(summary = "Fermer une conversation")
    public ResponseEntity<ConversationDto> closeConversation(
            @PathVariable Integer id,
            @AuthenticationPrincipal UsersDetailsAdapter principal) {

        Integer currentUserId = principal.getUser().getId();
        ConversationDto conversation = conversationService.closeConversation(id, currentUserId);
        return ResponseEntity.ok(conversation);
    }
}
