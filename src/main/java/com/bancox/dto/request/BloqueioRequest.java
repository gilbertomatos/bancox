package com.bancox.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BloqueioRequest(
    @NotBlank @Size(max = 255) String motivo
) {}
