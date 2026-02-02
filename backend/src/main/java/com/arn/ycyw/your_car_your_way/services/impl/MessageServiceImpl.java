package com.arn.ycyw.your_car_your_way.services.impl;

import com.arn.ycyw.your_car_your_way.dto.MessageDto;
import com.arn.ycyw.your_car_your_way.entity.Conversation;
import com.arn.ycyw.your_car_your_way.entity.Message;
import com.arn.ycyw.your_car_your_way.entity.Role;
import com.arn.ycyw.your_car_your_way.entity.Users;
import com.arn.ycyw.your_car_your_way.exception.BusinessException;
import com.arn.ycyw.your_car_your_way.mapper.MessageMapper;
import com.arn.ycyw.your_car_your_way.reposiory.ConversationRepository;
import com.arn.ycyw.your_car_your_way.reposiory.MessageRepository;
import com.arn.ycyw.your_car_your_way.reposiory.UserRepository;
import com.arn.ycyw.your_car_your_way.services.MessageService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final MessageMapper messageMapper;

    public MessageServiceImpl(MessageRepository messageRepository,
                              ConversationRepository conversationRepository,
                              UserRepository userRepository,
                              MessageMapper messageMapper) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.messageMapper = messageMapper;
    }

    @Override
    public MessageDto sendMessage(MessageDto messageDto, Integer senderId) {
        Conversation conversation = conversationRepository.findById(messageDto.getConversationId())
                .orElseThrow(() -> new BusinessException("Conversation not found"));

        Users sender = userRepository.findById(senderId)
                .orElseThrow(() -> new BusinessException("Sender not found"));

        if (!canAccessConversation(conversation, sender)) {
            throw new AccessDeniedException("You cannot send messages to this conversation");
        }

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(messageDto.getContent())
                .isRead(false)
                .build();

        Message saved = messageRepository.save(message);
        return messageMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageDto> getMessagesByConversation(Integer conversationId, Integer currentUserId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException("Conversation not found"));

        Users currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (!canAccessConversation(conversation, currentUser)) {
            throw new AccessDeniedException("You cannot access this conversation");
        }

        List<Message> messages = messageRepository.findAllByConversation_IdOrderBySentAtAsc(conversationId);
        return messageMapper.toDtoList(messages);
    }

    @Override
    public void markMessagesAsRead(Integer conversationId, Integer currentUserId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException("Conversation not found"));

        Users currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (!canAccessConversation(conversation, currentUser)) {
            throw new AccessDeniedException("You cannot access this conversation");
        }

        messageRepository.markAllAsRead(conversationId, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getUnreadCount(Integer conversationId, Integer userId) {
        return messageRepository.countUnreadMessages(conversationId, userId);
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
}
