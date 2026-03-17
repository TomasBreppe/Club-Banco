package com.example.demo.util;

import java.time.LocalDate;
import java.time.YearMonth;

public class PeriodUtil {

    public static String currentPeriod() {
        YearMonth ym = YearMonth.now();
        return ym.toString(); // "2026-03"
    }

    public static LocalDate endOfMonth(String periodYYYYMM) {
        YearMonth ym = YearMonth.parse(periodYYYYMM);
        return ym.atEndOfMonth();
    }

    public static boolean isAlDia(LocalDate vigenciaHasta) {
        return vigenciaHasta != null && !vigenciaHasta.isBefore(LocalDate.now());
    }
}
