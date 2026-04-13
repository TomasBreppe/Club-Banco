package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "disciplina")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisciplinaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String nombre; // PATIN, BASQUET, RITMICA, HANDBALL, TAEKWONDO

    @Column(nullable = false)
    private Boolean activa = true;

    @OneToMany(mappedBy = "disciplina")
    @Builder.Default
    private java.util.List<SocioDisciplinaEntity> socioDisciplinas = new java.util.ArrayList<>();
}
