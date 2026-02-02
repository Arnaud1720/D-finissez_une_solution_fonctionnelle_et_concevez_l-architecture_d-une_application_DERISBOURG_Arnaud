package com.arn.ycyw.your_car_your_way.controller;

import com.arn.ycyw.your_car_your_way.dto.AuthResponseDto;
import com.arn.ycyw.your_car_your_way.dto.LoginRequestDto;
import com.arn.ycyw.your_car_your_way.dto.UserDto;
import com.arn.ycyw.your_car_your_way.entity.Role;
import com.arn.ycyw.your_car_your_way.entity.Users;
import com.arn.ycyw.your_car_your_way.entity.VerificationStatus;
import com.arn.ycyw.your_car_your_way.mapper.UserMapper;
import com.arn.ycyw.your_car_your_way.security.UsersDetailsAdapter;
import com.arn.ycyw.your_car_your_way.services.impl.JwtServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtServiceImpl jwtService;
    private final UserMapper userMapper;


    public AuthController(AuthenticationManager authenticationManager, JwtServiceImpl jwtService, UserMapper userMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto dto) {
        // 1) Authentification via Spring Security
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(dto.getEmail(),dto.getPassword()));
        // 2) On récupère le principal = notre adapter
        UsersDetailsAdapter principal = (UsersDetailsAdapter) authentication.getPrincipal();
        // 3) On récupère l'entité Users
        assert principal != null;
        Users users = principal.getUser();

        // 4) Verification du statut pour les professionnels
        if (users.getRole() == Role.EMPLOYEE) {
            if (users.getVerificationStatus() == VerificationStatus.PENDING) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Votre compte professionnel est en attente de validation par l'administrateur");
            }
            if (users.getVerificationStatus() == VerificationStatus.REJECTED) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Votre demande de compte professionnel a ete refusee");
            }
        }

        // 5) On génère un token JWT
        String token = jwtService.generateToken(users.getEmail()); // ou getUsername()
        // 6) On construit éventuellement un UserDto
        UserDto userDto = userMapper.toDto(users);
        // 7) On renvoie tout ça dans un AuthResponseDto enveloppé dans un ResponseEntity
        AuthResponseDto response = new AuthResponseDto(token, "Bearer ", userDto);

        return ResponseEntity.ok(response);
    }
}
