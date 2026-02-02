package com.arn.ycyw.your_car_your_way.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteAccountRequestDto {
    
    @Schema(description = "Mot de passe pour confirmer la suppression du compte")
    @NotBlank(message = "Le mot de passe est obligatoire pour supprimer le compte")
    private String password;
}
