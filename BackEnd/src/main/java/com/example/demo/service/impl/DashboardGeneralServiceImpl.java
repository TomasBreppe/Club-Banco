package com.example.demo.service.impl;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.dashboard.DashboardGeneralDto;
import com.example.demo.repository.GastoRepository;
import com.example.demo.repository.PagoRepository;
import com.example.demo.repository.SocioRepository;
import com.example.demo.service.DashboardGeneralService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DashboardGeneralServiceImpl implements DashboardGeneralService {

    private final SocioRepository socioRepository;
    private final PagoRepository pagoRepository;
    private final GastoRepository gastoRepository;

    @Override
    public BaseResponse<DashboardGeneralDto> obtenerDashboardGeneral() {
        LocalDate hoy = LocalDate.now();
        int anio = hoy.getYear();
        int mes = hoy.getMonthValue();

        Long sociosActivos = socioRepository.countActivos();
        BigDecimal ingresosMes = pagoRepository.totalMes(anio, mes);
        BigDecimal gastosMes = gastoRepository.totalMes(anio, mes);
        BigDecimal balanceMes = ingresosMes.subtract(gastosMes);

        DashboardGeneralDto dto = DashboardGeneralDto.builder()
                .sociosActivos(sociosActivos)
                .ingresosMes(ingresosMes)
                .gastosMes(gastosMes)
                .balanceMes(balanceMes)
                .build();

        return new BaseResponse<>("Dashboard general obtenido correctamente", 200, dto);
    }
}