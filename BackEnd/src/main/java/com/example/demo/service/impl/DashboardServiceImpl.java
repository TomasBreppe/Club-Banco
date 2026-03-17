package com.example.demo.service.impl;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.dashboard.DashboardSociosResponseDto;
import com.example.demo.dto.dashboard.DashboardSociosResumenDto;
import com.example.demo.dto.socio.SocioDto;
import com.example.demo.entity.SocioEntity;
import com.example.demo.mapper.SocioMapper;
import com.example.demo.repository.SocioRepository;
import com.example.demo.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final SocioRepository socioRepository;

    @Override
    public BaseResponse<DashboardSociosResponseDto> obtenerDashboardSocios(
            Long disciplinaId,
            Boolean activo,
            String estadoPago,
            String q
    ) {
        LocalDate hoy = LocalDate.now();

        String qTrim = (q == null || q.trim().isBlank()) ? null : q.trim();
        String qLower = qTrim == null ? null : qTrim.toLowerCase();
        String qRaw = qTrim;

        long totalSocios = socioRepository.count();
        long activos = socioRepository.countActivos();
        long inactivos = socioRepository.countInactivos();
        long alDia = socioRepository.countAlDia(hoy);
        long debe = socioRepository.countDebe(hoy);

        DashboardSociosResumenDto resumen = DashboardSociosResumenDto.builder()
                .totalSocios(totalSocios)
                .activos(activos)
                .inactivos(inactivos)
                .alDia(alDia)
                .debe(debe)
                .build();

        List<SocioEntity> socios = socioRepository.searchDashboard(
                disciplinaId,
                activo,
                estadoPago,
                qLower,
                qRaw,
                hoy
        );

        List<SocioDto> sociosDto = socios.stream()
                .map(SocioMapper::toDto)
                .toList();

        DashboardSociosResponseDto data = DashboardSociosResponseDto.builder()
                .resumen(resumen)
                .socios(sociosDto)
                .build();

        return new BaseResponse<>("Dashboard de socios obtenido correctamente", 200, data);
    }
}