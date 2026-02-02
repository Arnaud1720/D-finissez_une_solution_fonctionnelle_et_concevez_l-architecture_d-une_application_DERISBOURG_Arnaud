package com.arn.ycyw.your_car_your_way.mapper;

import com.arn.ycyw.your_car_your_way.dto.UserDto;
import com.arn.ycyw.your_car_your_way.entity.Users;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-01T01:51:35+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 22.0.1 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserDto toDto(Users user) {
        if ( user == null ) {
            return null;
        }

        UserDto.UserDtoBuilder userDto = UserDto.builder();

        userDto.id( user.getId() );
        userDto.firstName( user.getFirstName() );
        userDto.lastName( user.getLastName() );
        userDto.email( user.getEmail() );
        userDto.password( user.getPassword() );
        userDto.creationDate( user.getCreationDate() );
        userDto.dateOfBirth( user.getDateOfBirth() );
        userDto.username( user.getUsername() );
        userDto.role( user.getRole() );
        userDto.verificationStatus( user.getVerificationStatus() );

        return userDto.build();
    }

    @Override
    public Users toEntity(UserDto dto) {
        if ( dto == null ) {
            return null;
        }

        Users.UsersBuilder users = Users.builder();

        users.id( dto.getId() );
        users.firstName( dto.getFirstName() );
        users.lastName( dto.getLastName() );
        users.email( dto.getEmail() );
        users.password( dto.getPassword() );
        users.dateOfBirth( dto.getDateOfBirth() );
        users.creationDate( dto.getCreationDate() );
        users.username( dto.getUsername() );
        users.role( dto.getRole() );
        users.verificationStatus( dto.getVerificationStatus() );

        return users.build();
    }
}
