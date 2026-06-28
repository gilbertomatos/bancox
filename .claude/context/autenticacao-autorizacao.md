# autenticacao-autorizacao.md — Autenticação e Autorização BancoX
> JWT em cookie httpOnly + perfis de acesso (DA-26)
> Consultar ao implementar: login, logout, SecurityConfig, JwtCookieFilter, sec:authorize

---

## Stack

| Tecnologia   | Versão | Função                                    |
|--------------|--------|-------------------------------------------|
| Spring Security | 6.x | filtros, contexto de autenticação        |
| JJWT         | 0.12.x | geração e validação de tokens             |
| BCrypt       | —      | hash de senhas (strength 12)              |
| Bucket4j     | 8.x    | rate limiting no /auth/login (DA-15)      |

---

## Perfis de Acesso

| Perfil      | Quem é                    | conta_id no token? |
|-------------|---------------------------|--------------------|
| CORRENTISTA | Cliente do banco          | Sim                |
| OPERADOR    | Funcionário administrativo| Não                |
| ADMIN       | Administrador do sistema  | Não                |

---

## Fluxo JWT Cookie (DA-26)

```
1. POST /auth/login { cpf, senha }
   → Validar credenciais (BCrypt)
   → Gerar JWT (15 min): { sub, conta_id, perfil, iat, exp }
   → Set-Cookie: bancox_token=eyJ...; HttpOnly; Secure; SameSite=Strict; Max-Age=900
   → Redirecionar por perfil: CORRENTISTA→/ | OPERADOR→/admin/contas

2. Requisições subsequentes (forms, HTMX)
   → Browser envia cookie automaticamente
   → JwtCookieFilter lê cookie "bancox_token"
   → Valida assinatura e expiração com JWT_SECRET
   → Popula SecurityContext

3. POST /auth/logout
   → Apagar cookie (Max-Age=0)
   → Redirecionar para /login?logout
```

---

## Estrutura do JWT

```json
{
  "sub": "uuid-do-usuario",
  "conta_id": "uuid-da-conta",
  "perfil": "CORRENTISTA | OPERADOR | ADMIN",
  "iat": 1719324000,
  "exp": 1719324900
}
```

---

## Mapeamento de Endpoints por Perfil

| Endpoint                           | Perfil mínimo | Ownership? |
|------------------------------------|---------------|------------|
| POST /auth/login                   | público       | —          |
| POST /auth/logout                  | qualquer      | —          |
| /actuator/health, /actuator/info   | público       | —          |
| /actuator/**                       | ADMIN         | —          |
| POST /contas/{id}/credito          | CORRENTISTA   | Sim        |
| POST /contas/{id}/debito           | CORRENTISTA   | Sim        |
| GET  /contas/{id}/extrato          | CORRENTISTA   | Sim        |
| POST /transferencias               | CORRENTISTA   | Sim (origem)|
| PATCH /contas/{id}/bloquear        | OPERADOR      | Não        |
| PATCH /contas/{id}/desbloquear     | OPERADOR      | Não        |
| /admin/**                          | OPERADOR      | Não        |

**Regra de Ownership (DA-13):**
CORRENTISTA só acessa sua própria conta.
`conta_id` da URL deve ser igual ao `conta_id` do token JWT.
Violação → HTTP 403 ACESSO_NEGADO.
NÃO aceitar conta_id no body — sempre extrair do token.

---

## Erros de Autenticação/Autorização

| Código                | HTTP | Quando                                    |
|-----------------------|------|-------------------------------------------|
| TOKEN_AUSENTE         | 401  | Cookie não presente                       |
| TOKEN_INVALIDO        | 401  | JWT malformado ou assinatura inválida     |
| TOKEN_EXPIRADO        | 401  | JWT expirado                              |
| CREDENCIAIS_INVALIDAS | 401  | CPF ou senha incorretos                   |
| ACESSO_NEGADO         | 403  | Perfil insuficiente ou ownership violado  |

---

## Regras de Implementação (OWASP)

```
NÃO armazenar senha em texto puro — BCrypt strength 12 obrigatório
NÃO revelar se foi CPF ou senha que errou — mensagem genérica sempre
NÃO permitir mais de 5 tentativas de login por CPF/minuto (Bucket4j — DA-15)
NÃO usar EnumType.ORDINAL para perfil — usar EnumType.STRING (DA-29)
NÃO logar CPF, senha_hash ou tokens nos logs
NÃO usar JWT_SECRET hardcoded — sempre variável de ambiente (DA-14)
NÃO habilitar CSRF — SameSite=Strict no cookie JWT protege (DA-26)
NÃO criar HttpSession — SessionCreationPolicy deve ser STATELESS
NÃO usar @Query com nativeQuery sem parâmetros nomeados (SQL Injection)
NÃO usar th:utext sem sanitização prévia (XSS)
```
