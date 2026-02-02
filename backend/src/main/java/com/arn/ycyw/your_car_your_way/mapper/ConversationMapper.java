package com.arn.ycyw.your_car_your_way.mapper;

import com.arn.ycyw.your_car_your_way.dto.ConversationDto;
import com.arn.ycyw.your_car_your_way.entity.Conversation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {MessageMapper.class})
public interface ConversationMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(target = "customerName", expression = "java(conversation.getCustomer().getFirstName() + \" \" + conversation.getCustomer().getLastName())")
    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(target = "employeeName", expression = "java(conversation.getEmployee() != null ? conversation.getEmployee().getFirstName() + \" \" + conversation.getEmployee().getLastName() : null)")
    @Mapping(target = "unreadCount", ignore = true)
    ConversationDto toDto(Conversation conversation);

    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "messages", ignore = true)
    Conversation toEntity(ConversationDto dto);

    List<ConversationDto> toDtoList(List<Conversation> conversations);
}
