package com.example.demo.service;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.dashboard.DashboardGeneralDto;
import org.springframework.stereotype.Service;

@Service
public interface DashboardGeneralService {
    BaseResponse<DashboardGeneralDto> obtenerDashboardGeneral();
}