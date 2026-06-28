# modelo-er/README.md — Modelo Entidade-Relacional BancoX
> Diagrama e índice. Restrições detalhadas vivem em cada arquivo de entidade.

## Diagrama

```
USUARIO   ||--o| CONTA          : "possui (CORRENTISTA)"
CLIENTE   ||--o{ CONTA          : "possui"
CONTA     ||--o{ LANCAMENTO     : "gera"
LANCAMENTO }o--o| TRANSFERENCIA : "origina de"
TRANSFERENCIA }o--|| CONTA      : "conta_origem"
TRANSFERENCIA }o--|| CONTA      : "conta_destino"
```

## Entidades

| Entidade      | Arquivo                    | Responsabilidade                              |
|---------------|----------------------------|-----------------------------------------------|
| USUARIO       | @entidade-usuario.md       | Credenciais e perfil de acesso                |
| CLIENTE       | @entidade-cliente.md       | Dados do titular da conta                     |
| CONTA         | @entidade-conta.md         | Saldo, status e vínculo com o cliente         |
| LANCAMENTO    | @entidade-lancamento.md    | Registro imutável de cada operação            |
| TRANSFERENCIA | @entidade-transferencia.md | Rastreabilidade de movimentações entre contas |

## Relação com os Casos de Uso

| Caso de Uso | Lê                | Escreve                                          |
|-------------|-------------------|--------------------------------------------------|
| UC-00       | USUARIO           | — (leitura apenas para autenticação)             |
| UC-01       | CONTA, USUARIO    | LANCAMENTO, CONTA.saldo                          |
| UC-02       | CONTA, USUARIO    | LANCAMENTO, CONTA.saldo                          |
| UC-03       | CONTA (x2), USUARIO | TRANSFERENCIA, LANCAMENTO (x2), CONTA.saldo (x2)|
| UC-04       | LANCAMENTO, CONTA, USUARIO | —                                      |
