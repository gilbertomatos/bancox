# UC-00 — Autenticação
> Pré-requisito para todos os outros UCs.
> Ver autenticacao-autorizacao.md para detalhes de JWT e perfis.

## User Story
Como usuário do BancoX (correntista ou operador)
Quero fazer login com meu CPF e senha
Para obter um token de acesso e operar o sistema de forma segura

## Critérios de Aceitação
- [ ] Login com CPF e senha válidos retorna access_token e refresh_token
- [ ] CPF ou senha inválidos retornam 401 CREDENCIAIS_INVALIDAS
- [ ] access_token expira em 15 minutos
- [ ] refresh_token renova o access_token sem novo login
- [ ] Logout invalida o refresh_token
- [ ] Todos os endpoints (exceto /auth/**) exigem Bearer token válido
- [ ] Token expirado retorna 401 TOKEN_EXPIRADO
- [ ] Token malformado retorna 401 TOKEN_INVALIDO

## Atores
Usuário → API (POST) → AuthService → UsuarioRepository + TokenService

## Fluxo Principal — Login

```
1. POST /auth/login
   Body: { "cpf": "12345678901", "senha": "senha123" }

2. AuthService valida:
   - cpf existe no sistema
   - senha confere com hash BCrypt armazenado

3. TokenService gera:
   - access_token (JWT, 15 min, contém: sub, conta_id, perfil, iat, exp)
   - refresh_token (UUID opaco, 7 dias, armazenado em cache)

4. Retorna HTTP 200
```

## Fluxo Principal — Renovação de Token

```
1. POST /auth/refresh
   Body: { "refresh_token": "uuid-opaco" }

2. TokenService valida:
   - refresh_token existe no cache
   - refresh_token não foi invalidado (logout)
   - refresh_token não expirou

3. TokenService gera novo access_token (15 min)

4. Retorna HTTP 200 com novo access_token
```

## Fluxo Principal — Logout

```
1. POST /auth/logout
   Header: Authorization: Bearer {access_token}

2. TokenService invalida o refresh_token do usuário no cache

3. Retorna HTTP 204 (sem corpo)
```

## Exceção — Credenciais Inválidas
```
→ HTTP 401 { "erro": "CREDENCIAIS_INVALIDAS",
              "mensagem": "CPF ou senha incorretos." }
NÃO revelar qual dos dois está errado (segurança)
```

## Exceção — Token Ausente
```
→ HTTP 401 { "erro": "TOKEN_AUSENTE",
              "mensagem": "Token de autenticação não informado." }
```

## Exceção — Token Inválido
```
→ HTTP 401 { "erro": "TOKEN_INVALIDO",
              "mensagem": "Token de autenticação inválido." }
```

## Exceção — Token Expirado
```
→ HTTP 401 { "erro": "TOKEN_EXPIRADO",
              "mensagem": "Token expirado. Renove via /auth/refresh." }
```

## Exceção — Refresh Token Inválido ou Expirado
```
→ HTTP 401 { "erro": "REFRESH_TOKEN_INVALIDO",
              "mensagem": "Sessão expirada. Faça login novamente." }
```

## Exemplos

### Login com sucesso — CORRENTISTA
```
ENTRADA : POST /auth/login { "cpf": "12345678901", "senha": "senha123" }
SAÍDA   : 200 {
            "status": "sucesso",
            "access_token": "eyJhbGci...",
            "refresh_token": "uuid-opaco-7dias",
            "expires_in": 900,
            "perfil": "CORRENTISTA"
          }
```

### Login com senha errada
```
ENTRADA : POST /auth/login { "cpf": "12345678901", "senha": "errada" }
SAÍDA   : 401 { "erro": "CREDENCIAIS_INVALIDAS",
                "mensagem": "CPF ou senha incorretos." }
ERRO A EVITAR: nunca informar se foi o CPF ou a senha que errou
```

### Requisição com token expirado
```
ENTRADA : POST /contas/abc-123/credito { "valor": 100.00 }
          Authorization: Bearer eyJhbGci... (expirado)
SAÍDA   : 401 { "erro": "TOKEN_EXPIRADO",
                "mensagem": "Token expirado. Renove via /auth/refresh." }
```

## Envelopes de Resposta

### Sucesso — Login (HTTP 200)
```json
{
  "status": "sucesso",
  "access_token": "eyJhbGci...",
  "refresh_token": "uuid-opaco",
  "expires_in": 900,
  "perfil": "CORRENTISTA | OPERADOR | ADMIN"
}
```

### Sucesso — Refresh (HTTP 200)
```json
{
  "status": "sucesso",
  "access_token": "eyJhbGci...",
  "expires_in": 900
}
```

## Critério de Sucesso
Token gerado com assinatura válida, expiração correta e perfil do usuário.
Refresh token armazenado no cache com TTL de 7 dias.
