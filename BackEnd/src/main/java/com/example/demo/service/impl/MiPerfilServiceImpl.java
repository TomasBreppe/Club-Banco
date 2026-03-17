package com.example.demo.service.impl;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.usuario.ActualizarFotoRequest;
import com.example.demo.dto.usuario.ActualizarPerfilRequest;
import com.example.demo.dto.usuario.MiPerfilDto;
import com.example.demo.entity.UsuarioEntity;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.MiPerfilService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MiPerfilServiceImpl implements MiPerfilService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public BaseResponse<MiPerfilDto> obtenerMiPerfil(String email) {
        UsuarioEntity usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return BaseResponse.ok("Perfil obtenido correctamente", toDto(usuario));
    }

    @Override
    public BaseResponse<MiPerfilDto> actualizarMiPerfil(String email, ActualizarPerfilRequest request) {
        UsuarioEntity usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        usuario.setNombre(trimToNull(request.getNombre()));
        usuario.setApellido(trimToNull(request.getApellido()));
        usuario.setTelefono(trimToNull(request.getTelefono()));

        UsuarioEntity saved = usuarioRepository.save(usuario);

        return BaseResponse.ok("Perfil actualizado correctamente", toDto(saved));
    }

    @Override
    public BaseResponse<MiPerfilDto> actualizarMiFoto(String email, ActualizarFotoRequest request) {
        UsuarioEntity usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String fotoUrl = request.getFotoUrl().trim();

        if (!esUrlValida(fotoUrl)) {
            return BaseResponse.bad("La URL de la imagen no es válida");
        }

        usuario.setFotoUrl(fotoUrl);

        UsuarioEntity saved = usuarioRepository.save(usuario);

        return BaseResponse.ok("Foto de perfil actualizada correctamente", toDto(saved));
    }

    private MiPerfilDto toDto(UsuarioEntity u) {
        return MiPerfilDto.builder()
                .id(u.getId())
                .email(u.getEmail())
                .rol(String.valueOf(u.getRol()))
                .activo(u.getActivo())
                .mustChangePassword(u.getMustChangePassword())
                .nombre(u.getNombre())
                .apellido(u.getApellido())
                .telefono(u.getTelefono())
                .fotoUrl(u.getFotoUrl())
                .build();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private boolean esUrlValida(String url) {
        String lower = url.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }
}
