package com.thoonidev.portfolio.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContactRequest(
        @NotBlank(message = "Ingresa tu nombre.")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres.")
        @Pattern(regexp = "^[^\\p{Cntrl}]*$", message = "El nombre contiene caracteres no permitidos.")
        String name,

        @NotBlank(message = "Ingresa tu correo.")
        @Email(message = "Ingresa un correo válido.")
        @Size(max = 254, message = "El correo es demasiado largo.")
        @Pattern(regexp = "^[^\\p{Cntrl}]*$", message = "El correo contiene caracteres no permitidos.")
        String email,

        @NotBlank(message = "Ingresa un asunto.")
        @Size(min = 3, max = 150, message = "El asunto debe tener entre 3 y 150 caracteres.")
        @Pattern(regexp = "^[^\\p{Cntrl}]*$", message = "El asunto debe ocupar una sola línea.")
        String subject,

        @NotBlank(message = "Escribe tu mensaje.")
        @Size(min = 10, max = 3000, message = "El mensaje debe tener entre 10 y 3000 caracteres.")
        String message,

        @Size(max = 200, message = "Campo no válido.")
        String website) {

    public ContactRequest {
        name = strip(name);
        email = strip(email);
        subject = strip(subject);
        message = strip(message);
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }
}
