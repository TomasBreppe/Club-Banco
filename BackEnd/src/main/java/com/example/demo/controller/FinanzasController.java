package com.example.demo.controller;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.arancel.ArancelCreateRequestDto;
import com.example.demo.dto.arancel.ArancelDisciplinaDto;
import com.example.demo.dto.deuda.DeudaResponseDto;
import com.example.demo.dto.pagos.PagoDto;
import com.example.demo.dto.pagos.PagoManualRequestDto;
import com.example.demo.dto.pagos.PagoManualResponseDto;
import com.example.demo.dto.socio.SocioResumenDto;
import com.example.demo.service.FinanzasService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FinanzasController {

    private final FinanzasService finanzasService;

    // ADMIN: ver deuda de cualquier socio
    @GetMapping("/admin/socios/{socioId}/deuda")
    public BaseResponse<DeudaResponseDto> deudaSocio(@PathVariable Long socioId) {
        return finanzasService.getDeudaBySocioId(socioId);
    }

    // SOCIO/ADMIN: deuda del socio logueado
    // (Acá depende de cómo armes tu JWT: si el token tiene socioId, usalo.
    // Si hoy no lo tenés, por ahora podés omitir este endpoint y quedarte con el de
    // admin.)
    @GetMapping("/me/deuda")
    public BaseResponse<DeudaResponseDto> miDeuda(Authentication auth) {
        // ✅ TODO: obtener socioId desde el usuario logueado.
        // Ejemplo (depende tu implementación):
        // Long socioId = ((CustomUserDetails) auth.getPrincipal()).getSocioId();

        return new BaseResponse<>("Falta vincular socioId al usuario logueado (JWT)", 400, null);
    }

    // ADMIN/caja: registrar pago manual
    @PostMapping("/admin/pagos/manual")
    public BaseResponse<PagoManualResponseDto> pagoManual(@Valid @RequestBody PagoManualRequestDto dto) {
        return finanzasService.registrarPagoManual(dto);
    }

    @GetMapping("/admin/socios/{socioId}/pagos")
    public BaseResponse<List<PagoDto>> pagosSocio(
            @PathVariable Long socioId,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta) {
        return finanzasService.historialPagosSocio(socioId, desde, hasta);
    }

    @GetMapping("/me/pagos")
    public BaseResponse<List<PagoDto>> misPagos(
            Authentication auth,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta) {
        return finanzasService.misPagos(auth.getName(), desde, hasta);
    }

    @GetMapping("/admin/socios/{socioId}/resumen-financiero")
    public BaseResponse<SocioResumenDto> resumenSocio(
            @PathVariable Long socioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer limit) {
        return finanzasService.resumenSocio(socioId, desde, hasta, limit);
    }

    @GetMapping("/admin/aranceles/activos")
    public BaseResponse<List<ArancelDisciplinaDto>> arancelesActivos() {
        return finanzasService.listarArancelesActivos();
    }

    @GetMapping("/admin/disciplinas/{disciplinaId}/aranceles")
    public BaseResponse<List<ArancelDisciplinaDto>> arancelesPorDisciplina(@PathVariable Long disciplinaId) {
        return finanzasService.listarArancelesPorDisciplina(disciplinaId);
    }

    @PostMapping("/admin/aranceles")
    public BaseResponse<ArancelDisciplinaDto> crearArancel(@RequestBody ArancelCreateRequestDto dto) {
        return finanzasService.crearArancel(dto);
    }

    @PatchMapping("/admin/aranceles/{arancelId}/estado")
    public BaseResponse<ArancelDisciplinaDto> cambiarEstadoArancel(
            @PathVariable Long arancelId,
            @RequestParam boolean activa) {
        return finanzasService.cambiarEstadoArancel(arancelId, activa);
    }

    @GetMapping("/admin/pagos/{pagoId}/comprobante")
    public ResponseEntity<byte[]> descargarComprobante(@PathVariable Long pagoId) {
        byte[] pdf = finanzasService.generarComprobantePdf(pagoId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=comprobante_" + pagoId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
