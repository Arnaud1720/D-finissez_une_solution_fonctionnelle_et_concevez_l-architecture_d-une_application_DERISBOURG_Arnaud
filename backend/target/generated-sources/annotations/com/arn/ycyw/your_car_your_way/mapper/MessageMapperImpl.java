package com.arn.ycyw.your_car_your_way.mapper;

import com.arn.ycyw.your_car_your_way.dto.MessageDto;
import com.arn.ycyw.your_car_your_way.entity.Conversation;
import com.arn.ycyw.your_car_your_way.entity.Message;
import com.arn.ycyw.your_car_your_way.entity.Users;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-01T01:51:35+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 22.0.1 (Oracle Corporation)"
)
@Component
public class MessageMapperImpl implements MessageMapper {

    @Override
    public MessageDto toDto(Message message) {
        if ( message == null ) {
            return null;
        }

        MessageDto.MessageDtoBuilder messageDto = MessageDto.builder();

        messageDto.conversationId( messageConversationId( message ) );
        messageDto.senderId( messageSenderId( message ) );
        messageDto.id( message.getId() );
        messageDto.content( message.getContent() );
        messageDto.sentAt( message.getSentAt() );
        messageDto.isRead( message.getIsRead() );

        messageDto.senderName( message.getSender().getFirstName() + " " + message.getSender().getLastName() );

        return messageDto.build();
    }

    @Override
    public Message toEntity(MessageDto dto) {
        if ( dto == null ) {
            return null;
        }

        Message.MessageBuilder message = Message.builder();

        message.id( dto.getId() );
        message.content( dto.getContent() );
        message.sentAt( dto.getSentAt() );
        message.isRead( dto.getIsRead() );

        return message.build();
    }

    @Override
    public List<MessageDto> toDtoList(List<Message> messages) {
        if ( messages == null ) {
            return null;
        }

        List<MessageDto> list = new ArrayList<MessageDto>( messages.size() );
        for ( Message message : messages ) {
            list.add( toDto( message ) );
        }

        return list;
    }

    private Integer messageConversationId(Message message) {
        if ( message == null ) {
            return null;
        }
        Conversation conversation = message.getConversation();
        if ( conversation == null ) {
            return null;
        }
        Integer id = conversation.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Integer messageSenderId(Message message) {
        if ( message == null ) {
            return null;
        }
        Users sender = message.getSender();
        if ( sender == null ) {
            return null;
        }
        Integer id = sender.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
