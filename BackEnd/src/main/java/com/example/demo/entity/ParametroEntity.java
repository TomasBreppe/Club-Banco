package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "parametro")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ParametroEntity {

    @Id
    @Column(length = 50)
    private String clave;

    @Column(name = "valor_num", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorNum;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
