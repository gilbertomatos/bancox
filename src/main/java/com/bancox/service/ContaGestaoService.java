package com.bancox.service;

import com.bancox.dto.response.ContaResumoResponse;
import com.bancox.dto.response.ContaStatusResponse;
import com.bancox.entity.StatusConta;
import com.bancox.exception.ContaJaAtivaException;
import com.bancox.exception.ContaJaBloqueadaException;
import com.bancox.exception.ContaNaoEncontradaException;
import com.bancox.exception.MotivoInvalidoException;
import com.bancox.repository.ClienteRepository;
import com.bancox.repository.ContaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContaGestaoService {

    private final ContaRepository contaRepository;
    private final ClienteRepository clienteRepository;

    @Transactional
    public ContaStatusResponse bloquear(UUID contaId, String motivo) {
        validarMotivo(motivo);

        var conta = contaRepository.findById(contaId)
            .orElseThrow(ContaNaoEncontradaException::new);

        if (conta.isBloqueada()) throw new ContaJaBloqueadaException();

        conta.bloquear(motivo);
        contaRepository.save(conta);

        return ContaStatusResponse.builder()
            .status("sucesso")
            .operacao("bloqueio")
            .contaId(contaId)
            .novoStatus("BLOQUEADA")
            .timestamp(Instant.now())
            .build();
    }

    @Transactional
    public ContaStatusResponse desbloquear(UUID contaId, String motivo) {
        validarMotivo(motivo);

        var conta = contaRepository.findById(contaId)
            .orElseThrow(ContaNaoEncontradaException::new);

        if (conta.isAtiva()) throw new ContaJaAtivaException();

        conta.desbloquear(motivo);
        contaRepository.save(conta);

        return ContaStatusResponse.builder()
            .status("sucesso")
            .operacao("desbloqueio")
            .contaId(contaId)
            .novoStatus("ATIVA")
            .timestamp(Instant.now())
            .build();
    }

    public List<ContaResumoResponse> listarTodas() {
        return contaRepository.findAllByOrderByCriadoEmDesc().stream()
            .map(this::toResumo)
            .toList();
    }

    public List<ContaResumoResponse> listarPorStatus(StatusConta status) {
        return contaRepository.findByStatusOrderByCriadoEmDesc(status).stream()
            .map(this::toResumo)
            .toList();
    }

    private ContaResumoResponse toResumo(com.bancox.entity.ContaEntity conta) {
        var cliente = clienteRepository.findById(conta.getClienteId()).orElse(null);
        return ContaResumoResponse.builder()
            .id(conta.getId())
            .status(conta.getStatus().name())
            .saldo(conta.getSaldo())
            .clienteNome(cliente != null ? cliente.getNome() : "")
            .clienteCpf(cliente != null ? cliente.getCpf() : "")
            .build();
    }

    private void validarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank() || motivo.length() > 255) {
            throw new MotivoInvalidoException();
        }
    }
}
