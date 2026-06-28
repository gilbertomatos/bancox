package com.bancox.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class OperacaoResponse {
    private final String status;
    private final String operacao;
    private final BigDecimal valor;
    @JsonProperty("saldo_anterior") private final BigDecimal saldoAnterior;
    @JsonProperty("saldo_atual")    private final BigDecimal saldoAtual;
    private final Instant timestamp;
}
