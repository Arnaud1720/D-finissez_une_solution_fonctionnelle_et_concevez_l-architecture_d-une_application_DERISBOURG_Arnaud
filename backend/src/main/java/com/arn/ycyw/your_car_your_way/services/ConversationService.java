package com.arn.ycyw.your_car_your_way.services;

import com.arn.ycyw.your_car_your_way.dto.ConversationDto;

import java.util.List;

public interface ConversationService {

    ConversationDto createConversation(ConversationDto conversationDto, Integer customerId);

    ConversationDto getConversationById(Integer id, Integer currentUserId);

    List<ConversationDto> getMyConversations(Integer userId);

    List<ConversationDto> getUnassignedConversations();

    ConversationDto assignEmployee(Integer conversationId, Integer employeeId);

    ConversationDto closeConversation(Integer conversationId, Integer currentUserId);
}
