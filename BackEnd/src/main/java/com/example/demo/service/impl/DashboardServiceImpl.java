package com.example.demo.service.impl;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.dashboard.DashboardSociosResponseDto;
import com.example.demo.dto.dashboard.DashboardSociosResumenDto;
import com.example.demo.dto.socio.SocioDto;
import com.example.demo.entity.SocioDisciplinaEntity;
import com.example.demo.entity.SocioEntity;
import com.example.demo.mapper.SocioMapper;
import com.example.demo.repository.SocioDisciplinaRepository;
import com.example.demo.repository.SocioRepository;
import com.example.demo.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

        private final SocioRepository socioRepository;
        private final SocioDisciplinaRepository socioDisciplinaRepository;

        @Override
        public BaseResponse<DashboardSociosResponseDto> obtenerDashboardSocios(
                        Long disciplinaId,
                        Boolean activo,
                        String categoria,
                        String estadoPago,
                        String q) {

                String qTrim = (q == null || q.trim().isBlank()) ? null : q.trim();
                String categoriaNorm = (categoria == null || categoria.isBlank()) ? null
                                : categoria.trim().toUpperCase();
                String estadoPagoNorm = (estadoPago == null || estadoPago.isBlank()) ? null
                                : estadoPago.trim().toUpperCase();

                LocalDate hoy = LocalDate.now();

                List<SocioEntity> sociosBase = socioRepository.searchDashboard(
                                disciplinaId,
                                activo,
                                categoriaNorm,
                                qTrim);

                List<Long> socioIds = sociosBase.stream()
                                .map(SocioEntity::getId)
                                .toList();

                List<SocioDisciplinaEntity> relaciones = socioIds.isEmpty()
                                ? List.of()
                                : socioDisciplinaRepository
                                                .findBySocio_IdInAndActivoTrueOrderBySocio_IdAscIdAsc(socioIds);

                Map<Long, SocioDisciplinaEntity> primeraRelacionPorSocio = new LinkedHashMap<>();
                Map<Long, Long> principalIdPorSocio = new LinkedHashMap<>();
                Map<Long, Boolean> deudaPorSocio = new LinkedHashMap<>();

                for (SocioDisciplinaEntity sd : relaciones) {
                        Long socioId = sd.getSocio().getId();
                        primeraRelacionPorSocio.putIfAbsent(socioId, sd);
                        principalIdPorSocio.putIfAbsent(socioId, sd.getId());
                }

                for (SocioDisciplinaEntity sd : relaciones) {
                        Long socioId = sd.getSocio().getId();
                        boolean esPrincipal = java.util.Objects.equals(principalIdPorSocio.get(socioId), sd.getId());

                        boolean debeInscripcion = esPrincipal && Boolean.FALSE.equals(sd.getInscripcionPagada());
                        boolean debeCuota = sd.getVigenciaHasta() == null || sd.getVigenciaHasta().isBefore(hoy);

                        deudaPorSocio.merge(socioId, (debeInscripcion || debeCuota), Boolean::logicalOr);
                }

                List<SocioDto> sociosDto = sociosBase.stream()
                                .map(s -> {
                                        SocioDisciplinaEntity sd = primeraRelacionPorSocio.get(s.getId());
                                        SocioDto dto = SocioMapper.toDto(s, sd);

                                        if (Boolean.FALSE.equals(s.getActivo())) {
                                                dto.setEstadoPago("INACTIVO");
                                                return dto;
                                        }

                                        boolean tieneDeuda = deudaPorSocio.getOrDefault(s.getId(), false);
                                        dto.setEstadoPago(tieneDeuda ? "DEBE" : "AL_DIA");
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