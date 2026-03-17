package com.example.demo.service;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.gasto.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public interface GastoService {

    BaseResponse<GastoDto> crear(GastoCreateDto dto);

    BaseResponse<List<GastoDto>> listar(
            String categoria,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String q
    );

    BaseResponse<DashboardGastosResponseDto> dashboard(
            String categoria,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String q
    );
}