package com.example.demo.controller.admin;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.socio.SocioAgregarDisciplinaDto;
import com.example.demo.dto.socio.SocioCreateDto;
import com.example.demo.dto.socio.SocioDto;
import com.example.demo.dto.socio.SocioResumenDto;
import com.example.demo.service.FinanzasService;
import com.example.demo.service.SocioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/socios")
@RequiredArgsConstructor
public class AdminSocioController {

    private final SocioService socioService;
    private final FinanzasService finanzasService;

    @PostMapping
    public BaseResponse<SocioDto> crear(@Valid @RequestBody SocioCreateDto dto) {
        return socioService.crear(dto);
    }

    @GetMapping
    public BaseResponse<List<SocioDto>> listar(
            @RequestParam(required = false) Long disciplinaId,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String estadoPago,
            @RequestParam(required = false) String q) {
        return socioService.listar(disciplinaId, categoria, estadoPago, q);
    }

    @GetMapping("/{id}/resumen")
    public BaseResponse<SocioResumenDto> resumen(@PathVariable Long id) {
        return finanzasService.resumenSocio(id, null, null, 10);
    }

    @PatchMapping("/{id}/activo")
    public BaseResponse<SocioDto> cambiarActivo(
            @PathVariable Long id,
            @RequestParam Boolean valor) {
        return socioService.cambiarActivo(id, valor);
    }

    @PostMapping("/{id}/disciplinas")
    public BaseResponse<SocioDto> agregarDisciplina(
            @PathVariable Long id,
            @RequestBody SocioAgregarDisciplinaDto dto) {
        return socioService.agregarDisciplina(id, dto);
    }
}