package com.example.demo.controller.admin;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.balance.DashboardBalanceResponseDto;
import com.example.demo.service.DashboardBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/dashboard/balance")
@RequiredArgsConstructor
public class AdminDashboardBalanceController {

    private final DashboardBalanceService dashboardBalanceService;

    @GetMapping
    public ResponseEntity<BaseResponse<DashboardBalanceResponseDto>> dashboard(
            @RequestParam(required = false) LocalDate fechaDesde,
            @RequestParam(required = false) LocalDate fechaHasta
    ) {
        return ResponseEntity.ok(dashboardBalanceService.dashboard(fechaDesde, fechaHasta));
    }
}