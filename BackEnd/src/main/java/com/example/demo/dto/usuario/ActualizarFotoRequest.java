package com.example.demo.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarFotoRequest {

    @NotBlank(message = "La URL de la foto es obligatoria")
    @Size(max = 500, message = "La URL no puede superar 500 caracteres")
    private String fotoUrl;
}
