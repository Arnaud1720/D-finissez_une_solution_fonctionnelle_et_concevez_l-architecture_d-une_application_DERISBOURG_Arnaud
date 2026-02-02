package com.arn.ycyw.your_car_your_way.mapper;

import com.arn.ycyw.your_car_your_way.dto.UserDto;
import com.arn.ycyw.your_car_your_way.entity.Users;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(Users user);
    Users toEntity(UserDto dto);

}
