package com.example.demo.controller.admin;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.gasto.*;
import com.example.demo.service.GastoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/gastos")
@RequiredArgsConstructor
public class AdminGastoController {

    private final GastoService gastoService;

    @PostMapping
    public ResponseEntity<BaseResponse<GastoDto>> crear(@RequestBody GastoCreateDto dto) {
        BaseResponse<GastoDto> response = gastoService.crear(dto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping
    public ResponseEntity<BaseResponse<List<GastoDto>>> listar(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String concepto,
            @RequestParam(required = false) LocalDate fechaDesde,
            @RequestParam(required = false) LocalDate fechaHasta,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(gastoService.listar(categoria, concepto, fechaDesde, fechaHasta, q));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<BaseResponse<DashboardGastosResponseDto>> dashboard(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String concepto,
            @RequestParam(required = false) LocalDate fechaDesde,
            @RequestParam(required = false) LocalDate fechaHasta,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(gastoService.dashboard(categoria, concepto, fechaDesde, fechaHasta, q));
    }
}