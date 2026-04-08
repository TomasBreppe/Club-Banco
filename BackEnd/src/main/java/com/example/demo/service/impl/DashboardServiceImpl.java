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
                        String categoria,
                        String estadoPago,
                        String q) {
                String qTrim = (q == null || q.trim().isBlank()) ? null : q.trim();
                String qLower = qTrim == null ? null : qTrim.toLowerCase();
                String categoriaNorm = (categoria == null || categoria.isBlank()) ? null
                                : categoria.trim().toUpperCase();
                String estadoPagoNorm = (estadoPago == null || estadoPago.isBlank()) ? null
                                : estadoPago.trim().toUpperCase();

                LocalDate hoy = LocalDate.now();

                List<SocioEntity> sociosBase = socioRepository.searchDashboard(
                                disciplinaId,
                                activo,
                                categoriaNorm,
                                qLower,
                                qTrim);

                List<SocioDto> sociosDto = sociosBase.stream()
                                .map(s -> {
                                        SocioDto dto = SocioMapper.toDto(s);

                                        if (Boolean.FALSE.equals(s.getActivo())) {
                                                dto.setEstadoPago("INACTIVO");
                                                return dto;
                                        }

                                        if (s.getVigenciaHasta() != null && !s.getVigenciaHasta().isBefore(hoy)) {
                                                dto.setEstadoPago("AL_DIA");
                                        } else {
                                                dto.setEstadoPago("DEBE");
                                        }

                                        return dto;
                                })
                                .filter(s -> estadoPagoNorm == null || estadoPagoNorm.equals(s.getEstadoPago()))
                                .toList();

                long totalSocios = sociosDto.size();
                long activosCount = sociosDto.stream().filter(s -> Boolean.TRUE.equals(s.getActivo())).count();
                long inactivosCount = sociosDto.stream().filter(s -> Boolean.FALSE.equals(s.getActivo())).count();
                long alDia = sociosDto.stream().filter(s -> "AL_DIA".equals(s.getEstadoPago())).count();
                long debe = sociosDto.stream().filter(s -> "DEBE".equals(s.getEstadoPago())).count();

                DashboardSociosResumenDto resumen = DashboardSociosResumenDto.builder()
                                .totalSocios(totalSocios)
                                .activos(activosCount)
                                .inactivos(inactivosCount)
                                .alDia(alDia)
                                .debe(debe)
                                .build();

                DashboardSociosResponseDto data = DashboardSociosResponseDto.builder()
                                .resumen(resumen)
                                .socios(sociosDto)
                                .build();

                return new BaseResponse<>("Dashboard de socios obtenido correctamente", 200, data);
        }
}