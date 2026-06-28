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
@Table(name = "conta")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal saldo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusConta status;

    @Column(name = "status_motivo", length = 255)
    private String statusMotivo;

    @Column(name = "status_atualizado_em")
    private Instant statusAtualizadoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    public void atualizarSaldo(BigDecimal novoSaldo) {
        this.saldo = novoSaldo;
    }

    public void bloquear(String motivo) {
        this.status = StatusConta.BLOQUEADA;
        this.statusMotivo = motivo;
        this.statusAtualizadoEm = Instant.now();
    }

    public void desbloquear(String motivo) {
        this.status = StatusConta.ATIVA;
        this.statusMotivo = motivo;
        this.statusAtualizadoEm = Instant.now();
    }

    public boolean isAtiva() {
        return StatusConta.ATIVA == this.status;
    }

    public boolean isBloqueada() {
        return StatusConta.BLOQUEADA == this.status;
    }
}
