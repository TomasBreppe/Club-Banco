package com.example.demo.dto.usuario;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UsuarioDto {
    private Long id;
    private String email;
    private String rol;
    private Boolean activo;
    private Boolean mustChangePassword;
}

