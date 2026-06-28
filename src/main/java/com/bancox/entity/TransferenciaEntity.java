package com.bancox.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transferencia")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferenciaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conta_origem", nullable = false)
    private UUID contaOrigem;

    @Column(name = "conta_destino", nullable = false)
    private UUID contaDestino;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;
}
