package com.bancox.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Pattern(regexp = "\\d{11}") String cpf,
    @NotBlank @Size(min = 6, max = 100) String senha
) {}
