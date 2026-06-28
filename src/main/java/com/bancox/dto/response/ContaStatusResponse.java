package com.bancox.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ContaStatusResponse {
    private final String status;
    private final String operacao;
    @JsonProperty("conta_id")    private final UUID contaId;
    @JsonProperty("novo_status") private final String novoStatus;
    private final Instant timestamp;
}
