package com.bancox.controller;

import com.bancox.dto.request.OperacaoRequest;
import com.bancox.dto.response.ExtratoResponse;
import com.bancox.dto.response.OperacaoResponse;
import com.bancox.security.UserPrincipal;
import com.bancox.service.ContaService;
import com.bancox.service.ExtratoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Conta", description = "Operações financeiras de conta corrente")
public class ContaController {

    private final ContaService contaService;
    private final ExtratoService extratoService;

    @Operation(summary = "Realizar crédito (depósito) em conta")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Crédito realizado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "Acesso negado"),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
        @ApiResponse(responseCode = "422", description = "Valor inválido ou conta bloqueada")
    })
    @PostMapping("/contas/{contaId}/credito")
    public ResponseEntity<OperacaoResponse> credito(
            @PathVariable UUID contaId,
            @Valid @RequestBody OperacaoRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(contaService.credito(contaId, principal, request.valor()));
    }

    @Operation(summary = "Realizar débito (saque/pagamento) em conta")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Débito realizado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "Acesso negado"),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
        @ApiResponse(responseCode = "422", description = "Valor inválido, saldo insuficiente ou conta bloqueada")
    })
    @PostMapping("/contas/{contaId}/debito")
    public ResponseEntity<OperacaoResponse> debito(
            @PathVariable UUID contaId,
            @Valid @RequestBody OperacaoRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(contaService.debito(contaId, principal, request.valor()));
    }

    @Operation(summary = "Consultar extrato da conta")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Extrato retornado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
        @ApiResponse(responseCode = "403", description = "Acesso negado"),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
        @ApiResponse(responseCode = "422", description = "Intervalo de datas inválido")
    })
    @GetMapping("/contas/{contaId}/extrato")
    public ResponseEntity<ExtratoResponse> extrato(
            @PathVariable UUID contaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(extratoService.extrato(contaId, principal, dataInicio, dataFim));
    }
}
