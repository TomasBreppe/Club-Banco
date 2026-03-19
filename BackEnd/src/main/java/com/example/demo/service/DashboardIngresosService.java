package com.example.demo.service;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.pagos.DashboardIngresosResponseDto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public interface DashboardIngresosService {
    BaseResponse<DashboardIngresosResponseDto> dashboard(
            String medio,
            Long disciplinaId,
            String categoriaManual,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String q
    );
}