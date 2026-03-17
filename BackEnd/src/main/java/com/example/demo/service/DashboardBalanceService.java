package com.example.demo.service;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.balance.DashboardBalanceResponseDto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public interface DashboardBalanceService {
    BaseResponse<DashboardBalanceResponseDto> dashboard(LocalDate fechaDesde, LocalDate fechaHasta);
}