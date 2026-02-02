package com.arn.ycyw.your_car_your_way.services;

import com.arn.ycyw.your_car_your_way.dto.UserDto;

import java.util.List;

public interface UserService {
    UserDto save(UserDto userDto);
    List<UserDto> findAll();
    UserDto findById(int id);
    void deleteById(int id);
    UserDto update(UserDto userDto);
    void deleteWithPassword(Integer userId, String password);

    /**
     * Verifie un compte professionnel via le token
     * @param token le token de verification
     * @param approve true pour approuver, false pour refuser
     * @return message de confirmation
     */
    String verifyEmployee(String token, boolean approve);
}
