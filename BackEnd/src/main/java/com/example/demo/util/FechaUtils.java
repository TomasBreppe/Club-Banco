package com.example.demo.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class FechaUtils {

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Cordoba");

    private FechaUtils() {}

    public static LocalDateTime ahoraArgentina() {
        return LocalDateTime.now(ZONA_ARGENTINA);
    }

    public static LocalDate hoyArgentina() {
        return LocalDate.now(ZONA_ARGENTINA);
    }
}