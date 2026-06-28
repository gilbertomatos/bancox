# design-system.md — Design System BancoX
> Bootstrap 5 + HTMX + Thymeleaf (server-side rendering)
> Consultar ao implementar qualquer tela ou template

---

## CDN — Incluir em fragments/layout.html (DA-24)

NÃO usar atributo `integrity` — hash pode divergir com atualizações silenciosas do CDN (DA-28).

```html
<link rel="stylesheet" crossorigin="anonymous"
  href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"/>
<link rel="stylesheet" crossorigin="anonymous"
  href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css"/>
<script crossorigin="anonymous"
  src="https://unpkg.com/htmx.org@2.0.0/dist/htmx.min.js"></script>
<script crossorigin="anonymous"
  src="https://cdn.jsdelivr.net/npm/chart.js@4.4.3/dist/chart.umd.min.js"></script>
<script crossorigin="anonymous"
  src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
```

---

## Tokens de Cor — Semântica Financeira

| Contexto              | Classe Bootstrap         |
|-----------------------|--------------------------|
| Crédito / positivo    | `text-success` `bg-success-subtle` |
| Débito / negativo     | `text-danger` `bg-danger-subtle`   |
| Transferência         | `text-secondary`         |
| Conta bloqueada       | `text-warning` `bg-warning-subtle` |
| Ação principal        | `btn-primary`            |
| Ação destrutiva       | `btn-danger`             |
| Ação neutra           | `btn-outline-secondary`  |

---

## Tipografia

| Elemento        | Classe Bootstrap           |
|-----------------|---------------------------|
| Título de página| `h4 fw-semibold`          |
| Título de seção | `h6 text-muted`           |
| Valor monetário | `font-monospace`          |
| Texto de apoio  | `small text-muted`        |

---

## Formatação Thymeleaf Obrigatória

```html
<!-- Moeda pt-BR: R$ 1.200,00 -->
th:text="${#numbers.formatCurrency(valor)}"

<!-- Data e hora: 25/06/2026 14:32 -->
th:text="${#temporals.format(data, 'dd/MM/yyyy HH:mm')}"

<!-- Data apenas: 25/06/2026 -->
th:text="${#temporals.format(data, 'dd/MM/yyyy')}"
```

---

## Componentes Principais

**Cartão de Saldo** — fundo `bg-primary`, valor em `font-monospace` branco 32px, badge `bg-warning` se conta BLOQUEADA.

**Linha de Lançamento no Extrato** — ícone `bi-arrow-up-circle-fill text-success` para crédito, `bi-arrow-down-circle-fill text-danger` para débito, `bi-arrow-left-right text-secondary` para transferência. Valor alinhado à direita com `font-monospace`.

**Feedback de Erro/Sucesso** — `alert alert-danger` para `${erro}`, `alert alert-success` para `${sucesso}`. Nunca exibir código técnico — apenas a mensagem legível.

**HTMX** — usar `hx-post`, `hx-target` e `hx-swap="outerHTML"` para atualizar fragmentos sem reload. Spinner `htmx-indicator` no botão de submit durante a requisição.

---

## Mapa de Telas

### Correntista
| Rota           | Template                        | UC-UI      |
|----------------|---------------------------------|------------|
| /login         | auth/login.html                 | UC-UI-04   |
| /              | correntista/dashboard.html      | UC-UI-01   |
| /extrato       | correntista/extrato.html        | UC-UI-02   |
| /transferencia | correntista/transferencia.html  | UC-UI-03   |

### Operador / Admin
| Rota           | Template                        | UC-UI      |
|----------------|---------------------------------|------------|
| /login         | auth/login.html                 | UC-UI-04   |
| /admin/contas  | operador/gestao-contas.html     | UC-UI-05   |

### Roteamento por perfil
- Não autenticado → `/login`
- CORRENTISTA → `/`
- OPERADOR ou ADMIN → `/admin/contas`
- CORRENTISTA tentando `/admin/**` → 403

---

## Estrutura de Templates

```
templates/
  fragments/   # layout.html, head.html, navbar.html, sidebar.html, alert.html
  auth/        # login.html
  correntista/ # dashboard.html, extrato.html, transferencia.html
  operador/    # gestao-contas.html
static/
  css/         # bancox.css (overrides Bootstrap)
  js/          # bancox.js (JS customizado mínimo)
```

---

## Padrões Obrigatórios

- `sec:authorize="hasRole('CORRENTISTA')"` para visibilidade por perfil nos templates
- `sec:authentication="name"` para exibir nome do usuário autenticado
- NÃO exibir código técnico de erro — usar `${erro}` e `${sucesso}` do Model
- NÃO instalar Bootstrap ou HTMX via npm — sempre via CDN (DA-24)
- NÃO duplicar lógica de autorização nos templates — confiar no Spring Security
