package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cuota_disciplina")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CuotaDisciplinaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "disciplina_id")
    private DisciplinaEntity disciplina;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde = LocalDate.now();

    @Column(nullable = false)
    private Boolean activa = true;
}
