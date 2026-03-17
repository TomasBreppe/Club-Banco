package com.example.demo.controller.admin;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.dashboard.DashboardSociosResponseDto;
import com.example.demo.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/socios")
    public ResponseEntity<BaseResponse<DashboardSociosResponseDto>> obtenerDashboardSocios(
            @RequestParam(required = false) Long disciplinaId,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String estadoPago,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(
                dashboardService.obtenerDashboardSocios(disciplinaId, activo, estadoPago, q)
        );
    }
}