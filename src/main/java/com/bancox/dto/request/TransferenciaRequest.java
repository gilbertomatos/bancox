package com.bancox.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferenciaRequest(
    @NotBlank String contaDestino,
    @NotNull @DecimalMin(value = "0.01") @Digits(integer = 13, fraction = 2) BigDecimal valor
) {}
