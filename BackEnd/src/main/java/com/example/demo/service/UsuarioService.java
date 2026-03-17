package com.example.demo.service;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.usuario.UsuarioCreateDto;
import com.example.demo.dto.usuario.UsuarioDto;
import org.springframework.stereotype.Service;

@Service
public interface UsuarioService {
    BaseResponse<UsuarioDto> crearResponsable(UsuarioCreateDto dto);
    BaseResponse<UsuarioDto> vincularSocio(Long usuarioId, Long socioId);
}
