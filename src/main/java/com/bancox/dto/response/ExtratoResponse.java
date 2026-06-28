package com.bancox.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class ExtratoResponse {
    private final String status;
    private final String operacao;
    private final String periodo;
    @JsonProperty("total_lancamentos") private final int totalLancamentos;
    private final List<LancamentoResponse> lancamentos;
    @JsonProperty("saldo_atual")        private final BigDecimal saldoAtual;
}
