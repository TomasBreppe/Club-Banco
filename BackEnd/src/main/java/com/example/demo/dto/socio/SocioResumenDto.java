package com.example.demo.dto.socio;

import com.example.demo.dto.deuda.DeudaResponseDto;
import com.example.demo.dto.pagos.PagoDto;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocioResumenDto {
    private Long socioId;
    private String dni;
    private String nombre;
    private String apellido;
    private String celular;
    private Boolean activo;

    // compatibilidad con front actual
    private LocalDate vigenciaHasta;
    private Long disciplinaId;
    private String disciplinaNombre;
    private Long arancelDisciplinaId;
    private String categoriaArancel;

    private Boolean tieneBeca;
    private BigDecimal porcentajeBecaSocial;
    private BigDecimal porcentajeBecaDeportiva;
    private BigDecimal porcentajeBecaPreparacionFisica;
    private String observacionBeca;
    
    private DeudaResponseDto deuda;
    private List<PagoDto> ultimosPagos;
    private List<SocioDisciplinaResumenDto> disciplinas;
}