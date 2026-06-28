# UC-UI-05 — Gestão de Contas (Operador / Admin)
> Template: operador/gestao-contas.html
> Rota: /admin/contas (GET, com HTMX para ações)
> UC de negócio: UC-05
> sec:authorize="hasAnyRole('OPERADOR', 'ADMIN')"

## User Story
Como operador autenticado
Quero visualizar e gerenciar status de contas
Para bloquear ou desbloquear em casos de suspeita de fraude

## Critérios de Aceitação
- [ ] Tabela Bootstrap com contas, status badge e ações
- [ ] Filtro por status via form GET
- [ ] Badge: bi-check-circle text-success (ATIVA) | bi-lock text-danger (BLOQUEADA)
- [ ] Botão "Bloquear" visível apenas para contas ATIVAS (th:if="${conta.ativa}")
- [ ] Botão "Desbloquear" visível apenas para contas BLOQUEADAS (th:if="${conta.bloqueada}")
- [ ] Modal Bootstrap com textarea de motivo (obrigatório, maxlength=255)
- [ ] Submit do modal via HTMX → atualiza a linha da tabela sem reload
- [ ] Histórico de auditoria acessível via collapse Bootstrap

## Dados disponíveis no template

| Variável     | Tipo   | Conteúdo                                           |
|--------------|--------|----------------------------------------------------|
| contas       | List   | lista de contas com id, cpf, saldo, status         |
| filtroStatus | String | filtro ativo: "ATIVA", "BLOQUEADA" ou null (todos) |

## HTMX para Bloqueio

```html
<!-- Botão que abre modal -->
<button class="btn btn-sm btn-outline-danger"
        data-bs-toggle="modal"
        th:attr="data-conta-id=${conta.id}">
  <i class="bi bi-lock"></i> Bloquear
</button>

<!-- Modal com form HTMX -->
<form hx-post="/admin/contas/bloquear"
      hx-target="#linha-conta-[[${conta.id}]]"
      hx-swap="outerHTML">
  <input type="hidden" name="contaId" th:value="${conta.id}"/>
  <textarea name="motivo" maxlength="255" required></textarea>
  <button type="submit" class="btn btn-danger">Confirmar bloqueio</button>
</form>
```
