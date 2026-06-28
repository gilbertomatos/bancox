package com.bancox.controller;

import com.bancox.dto.request.BloqueioRequest;
import com.bancox.dto.response.ContaStatusResponse;
import com.bancox.service.ContaGestaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Gestão de Contas", description = "Operações administrativas — requer perfil OPERADOR ou ADMIN")
public class AdminContaController {

    private final ContaGestaoService contaGestaoService;

    @Operation(summary = "Bloquear conta")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Conta bloqueada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "Perfil insuficiente"),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
        @ApiResponse(responseCode = "422", description = "Conta já bloqueada ou motivo inválido")
    })
    @PatchMapping("/contas/{contaId}/bloquear")
    public ResponseEntity<ContaStatusResponse> bloquear(
            @PathVariable UUID contaId,
            @Valid @RequestBody BloqueioRequest request) {

        return ResponseEntity.ok(contaGestaoService.bloquear(contaId, request.motivo()));
    }

    @Operation(summary = "Desbloquear conta")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Conta desbloqueada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "Perfil insuficiente"),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
        @ApiResponse(responseCode = "422", description = "Conta já ativa ou motivo inválido")
    })
    @PatchMapping("/contas/{contaId}/desbloquear")
    public ResponseEntity<ContaStatusResponse> desbloquear(
            @PathVariable UUID contaId,
            @Valid @RequestBody BloqueioRequest request) {

        return ResponseEntity.ok(contaGestaoService.desbloquear(contaId, request.motivo()));
    }
}
