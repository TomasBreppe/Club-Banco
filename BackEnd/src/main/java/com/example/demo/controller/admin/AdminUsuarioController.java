package com.example.demo.controller.admin;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.usuario.UsuarioCreateDto;
import com.example.demo.dto.usuario.UsuarioDto;
import com.example.demo.dto.usuario.VincularSocioDto;
import com.example.demo.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/usuarios")
@RequiredArgsConstructor
public class AdminUsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    public BaseResponse<UsuarioDto> crearResponsable(@Valid @RequestBody UsuarioCreateDto dto) {
        return usuarioService.crearResponsable(dto);
    }

    @PostMapping("/{usuarioId}/vincular-socio")
    public BaseResponse<UsuarioDto> vincularSocio(@PathVariable Long usuarioId,
                                                  @Valid @RequestBody VincularSocioDto dto) {
        return usuarioService.vincularSocio(usuarioId, dto.getSocioId());
    }
}
