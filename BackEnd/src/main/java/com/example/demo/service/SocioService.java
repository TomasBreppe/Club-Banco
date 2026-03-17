package com.example.demo.service;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.socio.SocioCreateDto;
import com.example.demo.dto.socio.SocioDto;
import com.example.demo.dto.socio.SocioResumenDto;
import org.springframework.stereotype.Service;
import com.example.demo.service.FinanzasService;
import java.math.BigDecimal;

import java.util.List;

@Service
public interface SocioService {
    BaseResponse<SocioDto> crear(SocioCreateDto dto);
    BaseResponse<List<SocioDto>> listar(Long disciplinaId, String estadoPago, String q);
    BaseResponse<SocioResumenDto> resumen(Long socioId);
    BaseResponse<SocioDto> cambiarActivo(Long socioId, Boolean activo);
}
