package com.arn.ycyw.your_car_your_way.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.arn.ycyw.your_car_your_way.entity.Role;
import com.arn.ycyw.your_car_your_way.entity.VerificationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    @Schema(description = "ID généré par le serveur", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer id;
    @Schema(description = "Prénom de l'utilisateur", example = "Arnaud")
//    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 20, message = "Votre prénom doit contenir entre 2 et 20 caractères")
    private String firstName;
    @Schema(description = "Nom de l'utilisateur", example = "Der")
    @Size(min = 2, max = 20, message = "Votre nom doit contenir entre 2 et 20 caractères")
    private String lastName;
    @Schema(description = "Adresse e-mail", example = "alice@example.com")
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Le format de l'email est invalide")
    private String email;
    @Schema(description = "Mot de passe (uniquement en écriture)", accessMode = Schema.AccessMode.WRITE_ONLY)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Pattern(
            regexp = "^(?=.*[#?!|_&]).{6,}$",
            message = "Le mot de passe doit comporter au moins 6 caractères et inclure au moins un caractère spécial parmi # ? ! | _ &"
    )
    private String password;
    @Schema(description = "date d'anniversaire auto incr")
    private LocalDateTime creationDate;
    private LocalDateTime dateOfBirth;
    @Schema(description = "pseudonyme",example = "arnaud1720")
    private String username;

    @Schema(description = "Type de compte: USER (client) ou EMPLOYEE (professionnel)", example = "USER")
    private Role role;

    @Schema(description = "Statut de verification pour les professionnels", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private VerificationStatus verificationStatus;
}
