package com.arn.ycyw.your_car_your_way.services;

import com.arn.ycyw.your_car_your_way.dto.MessageDto;

import java.util.List;

public interface MessageService {

    MessageDto sendMessage(MessageDto messageDto, Integer senderId);

    List<MessageDto> getMessagesByConversation(Integer conversationId, Integer currentUserId);

    void markMessagesAsRead(Integer conversationId, Integer currentUserId);

    Integer getUnreadCount(Integer conversationId, Integer userId);
}
