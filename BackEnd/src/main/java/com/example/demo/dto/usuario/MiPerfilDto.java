package com.example.demo.dto.usuario;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiPerfilDto {
    private Long id;
    private String email;
    private String rol;
    private Boolean activo;
    private Boolean mustChangePassword;

    private String nombre;
    private String apellido;
    private String telefono;
    private String fotoUrl;
}
