package com.example.demo.dto.usuario;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UsuarioCreateDto {
    @Email @NotBlank
    private String email;

    // si no la mandás, generamos una temporal
    private String password;
}
