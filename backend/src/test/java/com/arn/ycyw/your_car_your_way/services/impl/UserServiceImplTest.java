package com.arn.ycyw.your_car_your_way.services.impl;

import com.arn.ycyw.your_car_your_way.dto.UserDto;
import com.arn.ycyw.your_car_your_way.entity.Role;
import com.arn.ycyw.your_car_your_way.entity.Users;
import com.arn.ycyw.your_car_your_way.entity.VerificationStatus;
import com.arn.ycyw.your_car_your_way.mapper.UserMapper;
import com.arn.ycyw.your_car_your_way.reposiory.UserRepository;
import com.arn.ycyw.your_car_your_way.services.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserServiceImpl Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserServiceImpl userService;

    private UserDto userDto;
    private Users userEntity;

    @BeforeEach
    void setUp() {
        userDto = new UserDto();
        userDto.setEmail("test@example.com");
        userDto.setPassword("Test#123");
        userDto.setFirstName("John");
        userDto.setLastName("Doe");

        userEntity = new Users();
        userEntity.setId(1);
        userEntity.setEmail("test@example.com");
        userEntity.setFirstName("John");
        userEntity.setLastName("Doe");
    }

    @Nested
    @DisplayName("Tests de creation d'utilisateur")
    class SaveUserTests {

        @ParameterizedTest(name = "Creation avec role {0} doit retourner statut de verification {1}")
        @CsvSource({
            "USER, NONE",
            "EMPLOYEE, PENDING"
        })
        @DisplayName("Test parametrise: roles et statuts de verification")
        void save_WithDifferentRoles_ShouldSetCorrectVerificationStatus(Role role, VerificationStatus expectedStatus) {
            // Given
            userDto.setRole(role);
            userEntity.setRole(role);
            userEntity.setVerificationStatus(expectedStatus);

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(userMapper.toEntity(any(UserDto.class))).thenReturn(userEntity);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(Users.class))).thenReturn(userEntity);
            when(userMapper.toDto(any(Users.class))).thenReturn(userDto);

            // When
            UserDto result = userService.save(userDto);

            // Then
            ArgumentCaptor<Users> userCaptor = ArgumentCaptor.forClass(Users.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getVerificationStatus()).isEqualTo(expectedStatus);
        }

        @Test
        @DisplayName("Creation avec role ADMIN doit lancer une exception FORBIDDEN")
        void save_WithAdminRole_ShouldThrowForbiddenException() {
            // Given
            userDto.setRole(Role.ADMIN);

            // When & Then
            assertThatThrownBy(() -> userService.save(userDto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ADMIN");
        }

        @Test
        @DisplayName("Creation avec email existant doit lancer une exception CONFLICT")
        void save_WithExistingEmail_ShouldThrowConflictException() {
            // Given
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(userEntity));
            when(userMapper.toEntity(any(UserDto.class))).thenReturn(userEntity);

            // When & Then
            assertThatThrownBy(() -> userService.save(userDto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");
        }

        @ParameterizedTest(name = "Email {0} doit etre valide: {1}")
        @MethodSource("com.arn.ycyw.your_car_your_way.services.impl.UserServiceImplTest#provideEmailTestCases")
        @DisplayName("Test parametrise: validation des emails")
        void save_WithVariousEmails_ShouldHandleCorrectly(String email, boolean shouldSucceed) {
            // Given
            userDto.setEmail(email);
            userEntity.setEmail(email);

            if (shouldSucceed) {
                when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
                when(userMapper.toEntity(any(UserDto.class))).thenReturn(userEntity);
                when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
                when(userRepository.save(any(Users.class))).thenReturn(userEntity);
                when(userMapper.toDto(any(Users.class))).thenReturn(userDto);

                // When
                UserDto result = userService.save(userDto);

                // Then
                assertThat(result).isNotNull();
                verify(userRepository).save(any(Users.class));
            }
        }
    }

    @Nested
    @DisplayName("Tests de verification des employes")
    class VerifyEmployeeTests {

        @ParameterizedTest(name = "Verification avec approve={0} doit retourner statut {1}")
        @CsvSource({
            "true, VERIFIED",
            "false, REJECTED"
        })
        @DisplayName("Test parametrise: approbation ou rejet")
        void verifyEmployee_WithDifferentDecisions_ShouldSetCorrectStatus(boolean approve, VerificationStatus expectedStatus) {
            // Given
            String token = "valid-token";
            userEntity.setVerificationStatus(VerificationStatus.PENDING);
            userEntity.setVerificationToken(token);

            when(userRepository.findByVerificationToken(token)).thenReturn(Optional.of(userEntity));
            when(userRepository.save(any(Users.class))).thenReturn(userEntity);

            // When
            String result = userService.verifyEmployee(token, approve);

            // Then
            ArgumentCaptor<Users> userCaptor = ArgumentCaptor.forClass(Users.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getVerificationStatus()).isEqualTo(expectedStatus);
            assertThat(userCaptor.getValue().getVerificationToken()).isNull();

            if (approve) {
                assertThat(result).contains("approuve");
            } else {
                assertThat(result).contains("refuse");
            }
        }

        @Test
        @DisplayName("Verification avec token invalide doit lancer NOT_FOUND")
        void verifyEmployee_WithInvalidToken_ShouldThrowNotFoundException() {
            // Given
            when(userRepository.findByVerificationToken(anyString())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.verifyEmployee("invalid-token", true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("invalide");
        }

        @ParameterizedTest(name = "Verification avec statut {0} doit echouer")
        @EnumSource(value = VerificationStatus.class, names = {"VERIFIED", "REJECTED", "NONE"})
        @DisplayName("Test parametrise: statuts non-PENDING doivent echouer")
        void verifyEmployee_WithNonPendingStatus_ShouldThrowBadRequest(VerificationStatus status) {
            // Given
            userEntity.setVerificationStatus(status);
            when(userRepository.findByVerificationToken(anyString())).thenReturn(Optional.of(userEntity));

            // When & Then
            assertThatThrownBy(() -> userService.verifyEmployee("token", true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("deja ete traite");
        }
    }

    @Nested
    @DisplayName("Tests de suppression avec mot de passe")
    class DeleteWithPasswordTests {

        @ParameterizedTest(name = "Mot de passe {0} doit {1}")
        @CsvSource({
            "correctPassword, reussir",
            "wrongPassword, echouer"
        })
        @DisplayName("Test parametrise: verification du mot de passe")
        void deleteWithPassword_ShouldValidatePassword(String password, String expectedResult) {
            // Given
            Integer userId = 1;
            userEntity.setPassword("encodedCorrectPassword");

            when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));

            if (password.equals("correctPassword")) {
                when(passwordEncoder.matches(password, userEntity.getPassword())).thenReturn(true);

                // When
                userService.deleteWithPassword(userId, password);

                // Then
                verify(userRepository).delete(userEntity);
            } else {
                when(passwordEncoder.matches(password, userEntity.getPassword())).thenReturn(false);

                // When & Then
                assertThatThrownBy(() -> userService.deleteWithPassword(userId, password))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("incorrect");
            }
        }

        @Test
        @DisplayName("Suppression d'un utilisateur inexistant doit lancer NOT_FOUND")
        void deleteWithPassword_WithNonExistingUser_ShouldThrowNotFoundException() {
            // Given
            when(userRepository.findById(999)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.deleteWithPassword(999, "password"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("non");
        }
    }

    // Methode source pour les tests parametres d'email
    static Stream<Arguments> provideEmailTestCases() {
        return Stream.of(
            Arguments.of("valid@example.com", true),
            Arguments.of("user.name@domain.org", true),
            Arguments.of("test123@test.fr", true)
        );
    }
}
