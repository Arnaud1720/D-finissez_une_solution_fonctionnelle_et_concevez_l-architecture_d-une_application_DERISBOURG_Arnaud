package com.arn.ycyw.your_car_your_way.services.impl;

import com.arn.ycyw.your_car_your_way.dto.UserDto;
import com.arn.ycyw.your_car_your_way.entity.Role;
import com.arn.ycyw.your_car_your_way.entity.Users;
import com.arn.ycyw.your_car_your_way.entity.VerificationStatus;
import com.arn.ycyw.your_car_your_way.mapper.UserMapper;
import com.arn.ycyw.your_car_your_way.reposiory.UserRepository;
import com.arn.ycyw.your_car_your_way.services.EmailService;
import com.arn.ycyw.your_car_your_way.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper,
                          PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    public UserDto save(UserDto userDto) {
        LocalDateTime now = LocalDateTime.now();
        userDto.setId(null);

        // Validation du role: seulement USER ou EMPLOYEE autorise a l'inscription
        Role requestedRole = userDto.getRole();
        if (requestedRole == null) {
            requestedRole = Role.USER; // Par defaut
        } else if (requestedRole == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Le role ADMIN ne peut pas etre attribue lors de l'inscription");
        }

        Users entity = userMapper.toEntity(userDto);
        entity.setRole(requestedRole);
        entity.setCreationDate(now);

        boolean emailExist = userRepository.findByEmail(userDto.getEmail()).isPresent();
        if (emailExist) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
        }

        // Gestion de la verification pour les professionnels
        if (requestedRole == Role.EMPLOYEE) {
            entity.setVerificationStatus(VerificationStatus.PENDING);
            entity.setVerificationToken(UUID.randomUUID().toString());
        } else {
            entity.setVerificationStatus(VerificationStatus.NONE);
        }

        entity.setPassword(passwordEncoder.encode(userDto.getPassword()));
        Users saved = userRepository.save(entity);

        // Envoyer l'email de demande de validation a l'admin si c'est un EMPLOYEE
        if (requestedRole == Role.EMPLOYEE) {
            emailService.sendEmployeeValidationRequestToAdmin(saved, saved.getVerificationToken());
        }

        return userMapper.toDto(saved);
    }
    @Override
    public List<UserDto> findAll() {
        List<Users> users = userRepository.findAll();
        return users.stream().map(userMapper::toDto).collect(toList());
    }

    @Override
    public UserDto findById(int id) {
        Users user = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return userMapper.toDto(user);
    }

    @Override
    public void deleteById(int id) {
        userRepository.deleteById(id);
    }

    @Override
    public UserDto update(UserDto userDto) {
        // je vais cherche mon user par son id
        Users user = userRepository.findById(userDto.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setLastName(userDto.getLastName());
        user.setFirstName(userDto.getFirstName());
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    public void deleteWithPassword(Integer userId, String password) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Mot de passe incorrect");
        }

        userRepository.delete(user);
    }

    @Override
    public String verifyEmployee(String token, boolean approve) {
        Users user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Token de verification invalide ou deja utilise"));

        if (user.getVerificationStatus() != VerificationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce compte a deja ete traite");
        }

        if (approve) {
            user.setVerificationStatus(VerificationStatus.VERIFIED);
            user.setVerificationToken(null); // Invalider le token
            userRepository.save(user);
            emailService.sendEmployeeVerificationResult(user, true);
            return "Compte professionnel de " + user.getEmail() + " approuve avec succes";
        } else {
            user.setVerificationStatus(VerificationStatus.REJECTED);
            user.setVerificationToken(null);
            userRepository.save(user);
            emailService.sendEmployeeVerificationResult(user, false);
            return "Compte professionnel de " + user.getEmail() + " refuse";
        }
    }
}
