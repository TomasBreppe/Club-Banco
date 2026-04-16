package com.example.demo.controller.admin;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.ingresos.IngresoManualCreateDto;
import com.example.demo.dto.ingresos.IngresoManualDto;
import com.example.demo.dto.ingresos.IngresoManualUpdateRequestDto;
import com.example.demo.service.IngresoManualService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/ingresos-manuales")
@RequiredArgsConstructor
public class AdminIngresoManualController {

    private final IngresoManualService ingresoManualService;

    @PostMapping
    public ResponseEntity<BaseResponse<IngresoManualDto>> crear(@RequestBody IngresoManualCreateDto dto) {
        BaseResponse<IngresoManualDto> response = ingresoManualService.crear(dto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PutMapping("/{id}")
    public BaseResponse<IngresoManualDto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody IngresoManualUpdateRequestDto request) {
        return ingresoManualService.actualizar(id, request);
    }
}