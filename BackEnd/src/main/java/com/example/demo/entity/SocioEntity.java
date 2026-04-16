package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "socio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String dni;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String apellido;

    @Enumerated(EnumType.STRING)
    @Column(name = "genero", nullable = false)
    private Genero genero;

    @Column(length = 30)
    private String telefono;

    @Column(nullable = false, length = 30)
    private String celular;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "socio", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private java.util.List<SocioDisciplinaEntity> socioDisciplinas = new java.util.ArrayList<>();

    @Column(nullable = false)
    private Boolean tieneBeca = false;

    @Column(name = "porcentaje_beca_social", precision = 5, scale = 2)
    private BigDecimal porcentajeBecaSocial = BigDecimal.ZERO;

    @Column(name = "porcentaje_beca_deportiva", precision = 5, scale = 2)
    private BigDecimal porcentajeBecaDeportiva = BigDecimal.ZERO;

    @Column(name = "porcentaje_beca_preparacion_fisica", precision = 5, scale = 2)
    private BigDecimal porcentajeBecaPreparacionFisica = BigDecimal.ZERO;

    @Column(name = "observacion_beca", length = 255)
    private String observacionBeca;

    @PrePersist
    public void prePersist() {
        if (createdAt == null)
            createdAt = LocalDateTime.now();
        if (activo == null)
            activo = true;
    }
}