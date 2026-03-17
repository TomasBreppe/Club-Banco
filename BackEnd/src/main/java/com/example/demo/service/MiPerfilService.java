package com.example.demo.service;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.usuario.ActualizarFotoRequest;
import com.example.demo.dto.usuario.ActualizarPerfilRequest;
import com.example.demo.dto.usuario.MiPerfilDto;
import org.springframework.stereotype.Service;

@Service
public interface MiPerfilService {

    BaseResponse<MiPerfilDto> obtenerMiPerfil(String email);

    BaseResponse<MiPerfilDto> actualizarMiPerfil(String email, ActualizarPerfilRequest request);

    BaseResponse<MiPerfilDto> actualizarMiFoto(String email, ActualizarFotoRequest request);
}
