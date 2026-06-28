# UC-UI-02 — Extrato
> Template: correntista/extrato.html
> Rota: /extrato (GET com query params opcionais)
> UC de negócio: UC-04

## User Story
Como correntista autenticado
Quero consultar meu extrato com filtros de data
Para conferir minhas movimentações financeiras

## Critérios de Aceitação
- [ ] Tabela Bootstrap com lançamentos em ordem cronológica decrescente
- [ ] Filtros data_inicio e data_fim via form GET (sem HTMX — reload simples)
- [ ] Sem filtro: últimos 30 dias (padrão aplicado pelo controller)
- [ ] Ícone Bootstrap Icons por tipo: bi-arrow-up (crédito verde), bi-arrow-down (débito vermelho), bi-arrow-left-right (transferência)
- [ ] Valor alinhado à direita com font-monospace
- [ ] Saldo atual no topo via #numbers.formatCurrency
- [ ] Aviso Bootstrap alert-info quando lista vazia
- [ ] Gráfico Chart.js de barras (crédito vs débito por dia) — dados via th:attr="data-labels"
- [ ] Aviso quando limite de 50 lançamentos atingido

## Dados disponíveis no template

| Variável          | Tipo         | Conteúdo                                        |
|-------------------|--------------|-------------------------------------------------|
| lancamentos       | List         | lançamentos do período (máx 50)                 |
| saldoAtual        | BigDecimal   | saldo no momento da consulta                    |
| periodo           | String       | descrição do período consultado                 |
| totalLancamentos  | int          | quantidade de lançamentos retornados            |
| limiteAtingido    | boolean      | true quando totalLancamentos == 50              |
| labelsGrafico     | List<String> | datas do gráfico ["25/06", "26/06", ...]        |
| creditos          | List<Number> | valores de crédito por data do gráfico          |
| debitos           | List<Number> | valores de débito por data do gráfico           |

> Ver padrão de gráfico Chart.js em design-system.md
