package com.example.demo.controller;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.usuario.ActualizarFotoRequest;
import com.example.demo.dto.usuario.ActualizarPerfilRequest;
import com.example.demo.dto.usuario.MiPerfilDto;
import com.example.demo.service.MiPerfilService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MiPerfilController {

    private final MiPerfilService miPerfilService;

    @GetMapping("/perfil")
    public BaseResponse<MiPerfilDto> obtenerMiPerfil(Authentication auth) {
        return miPerfilService.obtenerMiPerfil(auth.getName());
    }

    @PutMapping("/perfil")
    public BaseResponse<MiPerfilDto> actualizarMiPerfil(Authentication auth,
                                                        @Valid @RequestBody ActualizarPerfilRequest request) {
        return miPerfilService.actualizarMiPerfil(auth.getName(), request);
    }

    @PutMapping("/foto")
    public BaseResponse<MiPerfilDto> actualizarMiFoto(Authentication auth,
                                                      @Valid @RequestBody ActualizarFotoRequest request) {
        return miPerfilService.actualizarMiFoto(auth.getName(), request);
    }
}
