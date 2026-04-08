package com.example.demo.service;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.arancel.ArancelCreateRequestDto;
import com.example.demo.dto.arancel.ArancelDisciplinaDto;
import com.example.demo.dto.deuda.DeudaResponseDto;
import com.example.demo.dto.pagos.PagoDto;
import com.example.demo.dto.pagos.PagoManualRequestDto;
import com.example.demo.dto.pagos.PagoManualResponseDto;
import com.example.demo.dto.socio.SocioResumenDto;
import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.util.List;

@Service
public interface FinanzasService {
    BaseResponse<DeudaResponseDto> getDeudaBySocioId(Long socioId);
    BaseResponse<PagoManualResponseDto> registrarPagoManual(PagoManualRequestDto dto);
    BaseResponse<List<PagoDto>> historialPagosSocio(Long socioId, LocalDate desde, LocalDate hasta);
    BaseResponse<List<PagoDto>> misPagos(String email, LocalDate desde, LocalDate hasta);
    BaseResponse<SocioResumenDto> resumenSocio(Long socioId, LocalDate desde, LocalDate hasta, Integer limit);
    BaseResponse<List<ArancelDisciplinaDto>> listarArancelesActivos();
    BaseResponse<List<ArancelDisciplinaDto>> listarArancelesPorDisciplina(Long disciplinaId);
    BaseResponse<ArancelDisciplinaDto> crearArancel(ArancelCreateRequestDto dto);
    BaseResponse<ArancelDisciplinaDto> cambiarEstadoArancel(Long arancelId, boolean activa);
    byte[] generarComprobantePdf(Long pagoId);
}
