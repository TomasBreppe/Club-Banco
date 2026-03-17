package com.example.demo.service;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.ingresos.IngresoManualCreateDto;
import com.example.demo.dto.ingresos.IngresoManualDto;
import org.springframework.stereotype.Service;

@Service
public interface IngresoManualService {
    BaseResponse<IngresoManualDto> crear(IngresoManualCreateDto dto);
}