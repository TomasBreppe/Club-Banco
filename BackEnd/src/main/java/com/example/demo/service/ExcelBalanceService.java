package com.example.demo.service;

import java.time.LocalDate;

public interface ExcelBalanceService {
    byte[] generarExcelBalance(LocalDate desde, LocalDate hasta);
}