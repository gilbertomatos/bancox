package com.bancox.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class LancamentoResponse {
    private final String tipo;
    private final BigDecimal valor;
    @JsonProperty("saldo_apos")  private final BigDecimal saldoApos;
    private final Instant timestamp;
    @JsonProperty("transfer_id") private final UUID transferId;
}
