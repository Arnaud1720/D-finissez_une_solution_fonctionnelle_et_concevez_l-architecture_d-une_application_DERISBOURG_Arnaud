package com.arn.ycyw.your_car_your_way.mapper;

import com.arn.ycyw.your_car_your_way.dto.MessageDto;
import com.arn.ycyw.your_car_your_way.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    @Mapping(source = "conversation.id", target = "conversationId")
    @Mapping(source = "sender.id", target = "senderId")
    @Mapping(target = "senderName", expression = "java(message.getSender().getFirstName() + \" \" + message.getSender().getLastName())")
    MessageDto toDto(Message message);

    @Mapping(target = "conversation", ignore = true)
    @Mapping(target = "sender", ignore = true)
    Message toEntity(MessageDto dto);

    List<MessageDto> toDtoList(List<Message> messages);
}
