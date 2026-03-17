package com.example.demo.service.impl;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.usuario.UsuarioCreateDto;
import com.example.demo.dto.usuario.UsuarioDto;
import com.example.demo.entity.RolUsuario;
import com.example.demo.entity.SocioEntity;
import com.example.demo.entity.UsuarioEntity;
import com.example.demo.repository.SocioRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.UsuarioService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final SocioRepository socioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public BaseResponse<UsuarioDto> crearResponsable(UsuarioCreateDto dto) {

        String email = dto.getEmail().trim().toLowerCase();

        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            return BaseResponse.bad("Ya existe un usuario con ese email");
        }

        String plainPass = (dto.getPassword() != null && !dto.getPassword().isBlank())
                ? dto.getPassword()
                : generarPasswordTemporal();

        UsuarioEntity u = UsuarioEntity.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(plainPass))
                .rol(RolUsuario.valueOf("SOCIO"))
                .activo(true)
                .mustChangePassword(true) // fuerza cambio primer login
                .build();

        UsuarioEntity saved = usuarioRepository.save(u);

        // OJO: por seguridad normalmente NO devolvés la password.
        // Para MVP interno de club, podrías mostrarla una sola vez desde Admin (si querés).
        // Por ahora solo devolvemos el usuario.
        return BaseResponse.created("Responsable creado (debe cambiar contraseña en el primer ingreso)", toDto(saved));
    }

    @Transactional
    @Override
    public BaseResponse<UsuarioDto> vincularSocio(Long usuarioId, Long socioId) {

        UsuarioEntity u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario inexistente"));

        String rol = String.valueOf(u.getRol());
        if (!"SOCIO".equals(rol)) {
            return BaseResponse.bad("Solo se puede vincular socios a usuarios con rol SOCIO. Rol actual=" + rol);
        }


        SocioEntity s = socioRepository.findById(socioId)
                .orElseThrow(() -> new IllegalArgumentException("Socio inexistente"));

        u.getSocios().add(s);
        UsuarioEntity saved = usuarioRepository.save(u);

        return BaseResponse.ok("Socio vinculado al responsable", toDto(saved));
    }

    private UsuarioDto toDto(UsuarioEntity u) {
        return UsuarioDto.builder()
                .id(u.getId())
                .email(u.getEmail())
                .rol(String.valueOf(u.getRol()))
                .activo(u.getActivo())
                .mustChangePassword(u.getMustChangePassword())
                .build();
    }

    private String generarPasswordTemporal() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }
}
