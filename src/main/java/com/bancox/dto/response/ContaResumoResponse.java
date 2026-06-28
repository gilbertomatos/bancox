package com.bancox.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class ContaResumoResponse {
    private final UUID id;
    private final String status;
    private final BigDecimal saldo;
    private final String clienteNome;
    private final String clienteCpf;
}
