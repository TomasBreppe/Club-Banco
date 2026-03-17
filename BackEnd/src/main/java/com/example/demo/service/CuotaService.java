package com.example.demo.service;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.cuota.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CuotaService {
    BaseResponse<CuotaDto> getActiva(Long disciplinaId);
    BaseResponse<CuotaDto> crearNueva(Long disciplinaId, CuotaCreateDto dto);
    BaseResponse<List<CuotaDto>> historial(Long disciplinaId);
}
