package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "socio_id")
    private SocioEntity socio;

    @ManyToOne
    @JoinColumn(name = "disciplina_id")
    private DisciplinaEntity disciplina;

    @ManyToOne
    @JoinColumn(name = "arancel_disciplina_id")
    private ArancelDisciplinaEntity arancelDisciplina;

    @Column(nullable = false, length = 40)
    private String concepto;

    @Column(length = 7)
    private String periodo;

    @Column(name = "categoria", length = 80)
    private String categoria;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "monto_social", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoSocial;

    @Column(name = "monto_disciplina", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoDisciplina;

    @Column(name = "monto_preparacion_fisica", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoPreparacionFisica;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "medio_pago")
    private MedioPago medio;

    @Column(name = "observacion", length = 255)
    private String observacion;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago;

    @Column(name = "mp_payment_id", length = 50)
    private String mpPaymentId;

    @Column(name = "mp_status", length = 30)
    private String mpStatus;
}