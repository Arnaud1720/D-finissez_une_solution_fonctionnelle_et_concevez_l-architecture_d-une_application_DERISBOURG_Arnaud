package com.arn.ycyw.your_car_your_way.mapper;

import com.arn.ycyw.your_car_your_way.dto.ConversationDto;
import com.arn.ycyw.your_car_your_way.entity.Conversation;
import com.arn.ycyw.your_car_your_way.entity.Users;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-01T01:51:35+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 22.0.1 (Oracle Corporation)"
)
@Component
public class ConversationMapperImpl implements ConversationMapper {

    @Autowired
    private MessageMapper messageMapper;

    @Override
    public ConversationDto toDto(Conversation conversation) {
        if ( conversation == null ) {
            return null;
        }

        ConversationDto.ConversationDtoBuilder conversationDto = ConversationDto.builder();

        conversationDto.customerId( conversationCustomerId( conversation ) );
        conversationDto.employeeId( conversationEmployeeId( conversation ) );
        conversationDto.id( conversation.getId() );
        conversationDto.subject( conversation.getSubject() );
        conversationDto.status( conversation.getStatus() );
        conversationDto.createdAt( conversation.getCreatedAt() );
        conversationDto.updatedAt( conversation.getUpdatedAt() );
        conversationDto.messages( messageMapper.toDtoList( conversation.getMessages() ) );

        conversationDto.customerName( conversation.getCustomer().getFirstName() + " " + conversation.getCustomer().getLastName() );
        conversationDto.employeeName( conversation.getEmployee() != null ? conversation.getEmployee().getFirstName() + " " + conversation.getEmployee().getLastName() : null );

        return conversationDto.build();
    }

    @Override
    public Conversation toEntity(ConversationDto dto) {
        if ( dto == null ) {
            return null;
        }

        Conversation.ConversationBuilder conversation = Conversation.builder();

        conversation.id( dto.getId() );
        conversation.subject( dto.getSubject() );
        conversation.status( dto.getStatus() );
        conversation.createdAt( dto.getCreatedAt() );
        conversation.updatedAt( dto.getUpdatedAt() );

        return conversation.build();
    }

    @Override
    public List<ConversationDto> toDtoList(List<Conversation> conversations) {
        if ( conversations == null ) {
            return null;
        }

        List<ConversationDto> list = new ArrayList<ConversationDto>( conversations.size() );
        for ( Conversation conversation : conversations ) {
            list.add( toDto( conversation ) );
        }

        return list;
    }

    private Integer conversationCustomerId(Conversation conversation) {
        if ( conversation == null ) {
            return null;
        }
        Users customer = conversation.getCustomer();
        if ( customer == null ) {
            return null;
        }
        Integer id = customer.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Integer conversationEmployeeId(Conversation conversation) {
        if ( conversation == null ) {
            return null;
        }
        Users employee = conversation.getEmployee();
        if ( employee == null ) {
            return null;
        }
        Integer id = employee.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
