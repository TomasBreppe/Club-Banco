package com.example.demo.dto.ingresos;

import com.example.demo.entity.CategoriaIngresoManual;
import com.example.demo.entity.MedioIngresoManual;
import com.example.demo.entity.MedioPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class IngresoManualUpdateRequestDto {

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La categoría es obligatoria")
    private CategoriaIngresoManual categoria;

    @NotNull(message = "El concepto es obligatorio")
    @Size(max = 120, message = "El concepto no puede superar los 120 caracteres")
    private String concepto;

    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    private String descripcion;

    @NotNull(message = "El medio de pago es obligatorio")
    private MedioIngresoManual medioPago;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal monto;
}