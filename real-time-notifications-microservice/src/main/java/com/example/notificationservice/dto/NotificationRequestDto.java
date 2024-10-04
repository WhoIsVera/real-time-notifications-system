package com.example.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestDto {
    // Asegurar que el que el campo no sea null y que no esté vacío
    @NotBlank(message = "El mensaje no puede estar vacío")
    private String message;

}
