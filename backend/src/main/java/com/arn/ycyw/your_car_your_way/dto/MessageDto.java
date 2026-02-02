package com.arn.ycyw.your_car_your_way.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDto {
    @Schema(description = "ID du message", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer id;

    @Schema(description = "ID de la conversation")
    @NotNull(message = "L'ID de la conversation est obligatoire")
    private Integer conversationId;

    @Schema(description = "ID de l'expediteur", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer senderId;

    @Schema(description = "Nom de l'expediteur", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String senderName;

    @Schema(description = "Contenu du message")
    @NotBlank(message = "Le contenu du message est obligatoire")
    private String content;

    @Schema(description = "Date d'envoi", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime sentAt;

    @Schema(description = "Message lu", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean isRead;
}
