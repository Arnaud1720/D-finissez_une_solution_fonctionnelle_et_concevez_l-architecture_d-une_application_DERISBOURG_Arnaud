package com.arn.ycyw.your_car_your_way.services.impl;

import com.arn.ycyw.your_car_your_way.dto.ConversationDto;
import com.arn.ycyw.your_car_your_way.entity.Conversation;
import com.arn.ycyw.your_car_your_way.entity.ConversationStatus;
import com.arn.ycyw.your_car_your_way.entity.Role;
import com.arn.ycyw.your_car_your_way.entity.Users;
import com.arn.ycyw.your_car_your_way.exception.BusinessException;
import com.arn.ycyw.your_car_your_way.mapper.ConversationMapper;
import com.arn.ycyw.your_car_your_way.reposiory.ConversationRepository;
import com.arn.ycyw.your_car_your_way.reposiory.MessageRepository;
import com.arn.ycyw.your_car_your_way.reposiory.UserRepository;
import com.arn.ycyw.your_car_your_way.services.ConversationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ConversationMapper conversationMapper;

    public ConversationServiceImpl(ConversationRepository conversationRepository,
                                   UserRepository userRepository,
                                   MessageRepository messageRepository,
                                   ConversationMapper conversationMapper) {
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.conversationMapper = conversationMapper;
    }

    @Override
    public ConversationDto createConversation(ConversationDto conversationDto, Integer customerId) {
        Users customer = userRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException("Customer not found"));

        Conversation conversation = Conversation.builder()
                .subject(conversationDto.getSubject())
                .customer(customer)
                .status(ConversationStatus.OPEN)
                .build();

        Conversation saved = conversationRepository.save(conversation);
        return toDto(saved, customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDto getConversationById(Integer id, Integer currentUserId) {
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Conversation not found"));

        Users currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (!canAccessConversation(conversation, currentUser)) {
            throw new AccessDeniedException("You cannot access this conversation");
        }

        return toDto(conversation, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationDto> getMyConversations(Integer userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        List<Conversation> conversations;

        if (user.getRole() == Role.EMPLOYEE || user.getRole() == Role.ADMIN) {
            conversations = conversationRepository.findAllByEmployee_IdOrderByUpdatedAtDesc(userId);
        } else {
            conversations = conversationRepository.findAllByCustomer_IdOrderByUpdatedAtDesc(userId);
        }

        return conversations.stream()
                .map(c -> toDto(c, userId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationDto> getUnassignedConversations() {
        List<Conversation> conversations = conversationRepository
                .findUnassignedConversations(ConversationStatus.OPEN);

        return conversationMapper.toDtoList(conversations);
    }

    @Override
    public ConversationDto assignEmployee(Integer conversationId, Integer employeeId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException("Conversation not found"));

        Users employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException("Employee not found"));

        if (employee.getRole() != Role.EMPLOYEE && employee.getRole() != Role.ADMIN) {
            throw new BusinessException("User is not an employee");
        }

        conversation.setEmployee(employee);
        Conversation saved = conversationRepository.save(conversation);
        return toDto(saved, employeeId);
    }

    @Override
    public ConversationDto closeConversation(Integer conversationId, Integer currentUserId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException("Conversation not found"));

        Users currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (!canAccessConversation(conversation, currentUser)) {
            throw new AccessDeniedException("You cannot close this conversation");
        }

        conversation.setStatus(ConversationStatus.CLOSED);
        Conversation saved = conversationRepository.save(conversation);
        return toDto(saved, currentUserId);
    }

    private boolean canAccessConversation(Conversation conversation, Users user) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (conversation.getCustomer().getId().equals(user.getId())) {
            return true;
        }
        if (conversation.getEmployee() != null && conversation.getEmployee().getId().equals(user.getId())) {
            return true;
        }
        if ((user.getRole() == Role.EMPLOYEE) && conversation.getEmployee() == null) {
            return true;
        }
        return false;
    }

    private ConversationDto toDto(Conversation conversation, Integer currentUserId) {
        ConversationDto dto = conversationMapper.toDto(conversation);
        Integer unreadCount = messageRepository.countUnreadMessages(conversation.getId(), currentUserId);
        dto.setUnreadCount(unreadCount);
        return dto;
    }
}
