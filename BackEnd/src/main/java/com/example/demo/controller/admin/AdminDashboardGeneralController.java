package com.example.demo.controller.admin;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.dashboard.DashboardGeneralDto;
import com.example.demo.service.DashboardGeneralService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardGeneralController {

    private final DashboardGeneralService dashboardGeneralService;

    @GetMapping("/general")
    public ResponseEntity<BaseResponse<DashboardGeneralDto>> obtenerDashboardGeneral() {
        return ResponseEntity.ok(dashboardGeneralService.obtenerDashboardGeneral());
    }
}