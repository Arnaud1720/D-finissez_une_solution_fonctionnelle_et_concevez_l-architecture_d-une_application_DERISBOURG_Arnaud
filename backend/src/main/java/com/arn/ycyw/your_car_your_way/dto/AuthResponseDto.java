package com.arn.ycyw.your_car_your_way.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
public class AuthResponseDto {
    private String token;
    private String tokenType;
    private UserDto user;

}
