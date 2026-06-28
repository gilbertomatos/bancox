package com.bancox.security;

import java.util.UUID;

public record UserPrincipal(UUID userId, UUID contaId, String perfil) {

    public boolean isCorrentista() {
        return "CORRENTISTA".equals(perfil);
    }

    public boolean possuiConta(UUID contaId) {
        return this.contaId != null && this.contaId.equals(contaId);
    }
}
