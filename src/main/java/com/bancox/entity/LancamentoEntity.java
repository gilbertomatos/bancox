package com.bancox.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Imutável por DA-03 — sem setters, sem UPDATE após INSERT
@Entity
@Table(name = "lancamento")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LancamentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conta_id", nullable = false)
    private UUID contaId;

    @Convert(converter = TipoLancamentoConverter.class)
    @Column(nullable = false, length = 20)
    private TipoLancamento tipo;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name = "saldo_apos", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoApos;

    @Column(name = "transfer_id")
    private UUID transferId;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;
}
