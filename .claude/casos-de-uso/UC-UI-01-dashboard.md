# UC-UI-01 — Dashboard do Correntista
> Template: correntista/dashboard.html
> Rota: / (GET)
> UCs de negócio: UC-01 (crédito), UC-02 (débito), UC-03 (transferência)

## User Story
Como correntista autenticado
Quero ver meu saldo e realizar operações na tela principal
Para operar minha conta sem navegar entre páginas

## Critérios de Aceitação
- [ ] Cartão de saldo exibe valor formatado em pt-BR via #numbers.formatCurrency
- [ ] Abas: Depósito | Saque | Transferência — uma operação por vez
- [ ] Formulários submetidos via HTMX — atualiza só o cartão de saldo (hx-target="#cartao-saldo")
- [ ] Feedback de sucesso/erro via fragment alert.html injetado pelo controller no Model
- [ ] Preview das últimas 5 movimentações abaixo das abas
- [ ] Botão "Ver extrato completo" → /extrato
- [ ] Conta BLOQUEADA: badge no cartão + formulários desabilitados (th:disabled="${conta.bloqueada}")
- [ ] Operador/Admin não vê esta tela — redirecionado para /admin/contas

## Dados disponíveis no template

| Variável    | Tipo        | Conteúdo                                      |
|-------------|-------------|-----------------------------------------------|
| conta       | ContaDTO    | id, saldo, status (ATIVA/BLOQUEADA)           |
| preview     | List        | últimas 5 movimentações                       |
| erro        | String      | mensagem de erro após redirect (opcional)     |
| sucesso     | String      | mensagem de sucesso após redirect (opcional)  |

## Fluxo HTMX de Operação

```
1. Usuário preenche formulário e submete
2. HTMX envia POST /contas/credito (ou /debito)
3. Controller processa → em caso de sucesso: retorna fragment atualizado do cartão
4. HTMX substitui #cartao-saldo com o novo HTML
5. Em caso de erro: retorna fragment alert com mensagem legível
```

## Estados da Tela

| Estado          | Template exibe                                    |
|-----------------|---------------------------------------------------|
| Carregando      | Spinner Bootstrap no lugar do cartão              |
| Conta ativa     | Formulários habilitados                           |
| Conta bloqueada | Badge warning + th:disabled nos inputs e botões   |
| Erro de operação| fragment alert.html com mensagem da API           |
| Sucesso         | fragment alert.html verde + cartão atualizado     |
