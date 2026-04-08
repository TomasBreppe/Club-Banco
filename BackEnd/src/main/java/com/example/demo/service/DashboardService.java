package com.example.demo.service;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.dashboard.DashboardSociosResponseDto;
import org.springframework.stereotype.Service;

@Service
public interface DashboardService {
    BaseResponse<DashboardSociosResponseDto> obtenerDashboardSocios(
            Long disciplinaId,
            Boolean activo,
            String categoria,
            String estadoPago,
            String q);
}