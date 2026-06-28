# UC-UI-04 — Login
> Template: auth/login.html
> Rota: /login (pública — Spring Security)
> UC de negócio: UC-00

## User Story
Como usuário do BancoX
Quero fazer login com CPF e senha
Para acessar o sistema de forma segura

## Critérios de Aceitação
- [ ] Campos: CPF e senha (type=password)
- [ ] Spring Security gerencia o POST /login automaticamente
- [ ] Erro de credenciais: parâmetro ?error na URL → exibir mensagem genérica
- [ ] Logout: parâmetro ?logout na URL → exibir confirmação
- [ ] Sessão gerenciada pelo Spring Security (sem JWT no frontend — DA-24)
- [ ] Redirecionamento por perfil via AuthenticationSuccessHandler

## Redirecionamento por Perfil

Implementar AuthenticationSuccessHandler:
- CORRENTISTA → /
- OPERADOR    → /admin/contas
- ADMIN       → /admin/contas
