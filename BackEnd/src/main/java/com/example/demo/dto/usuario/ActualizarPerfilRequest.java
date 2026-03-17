package com.example.demo.dto.usuario;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarPerfilRequest {

    @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
    private String nombre;

    @Size(max = 80, message = "El apellido no puede superar 80 caracteres")
    private String apellido;

    @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
    private String telefono;
}
