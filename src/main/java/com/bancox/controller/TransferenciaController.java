package com.bancox.controller;

import com.bancox.dto.request.TransferenciaRequest;
import com.bancox.dto.response.OperacaoResponse;
import com.bancox.security.UserPrincipal;
import com.bancox.service.TransferenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/transferencias")
@RequiredArgsConstructor
@Tag(name = "Transferência", description = "Transferências entre contas")
public class TransferenciaController {

    private final TransferenciaService transferenciaService;

    @Operation(summary = "Realizar transferência entre contas")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transferência realizada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "Acesso negado"),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
        @ApiResponse(responseCode = "422", description = "Valor inválido, saldo insuficiente, contas idênticas ou conta bloqueada"),
        @ApiResponse(responseCode = "500", description = "Falha na transação atômica")
    })
    @PostMapping
    public ResponseEntity<OperacaoResponse> transferir(
            @Valid @RequestBody TransferenciaRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        UUID contaDestinoId = UUID.fromString(request.contaDestino());
        return ResponseEntity.ok(
            transferenciaService.transferir(principal.contaId(), principal, contaDestinoId, request.valor())
        );
    }
}
