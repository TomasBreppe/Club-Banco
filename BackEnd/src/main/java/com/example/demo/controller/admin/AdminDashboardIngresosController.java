package com.example.demo.controller.admin;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.pagos.DashboardIngresosResponseDto;
import com.example.demo.service.DashboardIngresosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/dashboard/ingresos")
@RequiredArgsConstructor
public class AdminDashboardIngresosController {

    private final DashboardIngresosService dashboardIngresosService;

    @GetMapping
    public ResponseEntity<BaseResponse<DashboardIngresosResponseDto>> dashboard(
            @RequestParam(required = false) String medio,
            @RequestParam(required = false) LocalDate fechaDesde,
            @RequestParam(required = false) LocalDate fechaHasta,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(
                dashboardIngresosService.dashboard(medio, fechaDesde, fechaHasta, q)
        );
    }
}