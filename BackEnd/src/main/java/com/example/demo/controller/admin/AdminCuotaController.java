package com.example.demo.controller.admin;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.cuota.*;
import com.example.demo.service.CuotaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/disciplinas/{disciplinaId}/cuotas")
@RequiredArgsConstructor
public class AdminCuotaController {

    private final CuotaService cuotaService;

    @GetMapping("/activa")
    public BaseResponse<CuotaDto> activa(@PathVariable Long disciplinaId) {
        return cuotaService.getActiva(disciplinaId);
    }

    @GetMapping
    public BaseResponse<List<CuotaDto>> historial(@PathVariable Long disciplinaId) {
        return cuotaService.historial(disciplinaId);
    }

    @PostMapping
    public BaseResponse<CuotaDto> crear(@PathVariable Long disciplinaId, @Valid @RequestBody CuotaCreateDto dto) {
        return cuotaService.crearNueva(disciplinaId, dto);
    }
}
