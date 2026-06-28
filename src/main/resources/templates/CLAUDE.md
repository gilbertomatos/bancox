# CLAUDE.md — src/main/resources/templates/

## Regras desta camada

- Templates são finos: exibem dados do Model — sem lógica de negócio
- NÃO colocar cálculos ou regras de negócio em templates — mover para o controller ou service
- NÃO exibir mensagens de erro técnicas — usar variáveis ${erro} e ${sucesso} do Model
- NÃO exibir códigos de erro (SALDO_INSUFICIENTE) — apenas a mensagem legível
- NÃO duplicar lógica de autorização nos templates — usar sec:authorize
- Toda interatividade leve via HTMX — JS puro apenas quando HTMX não atender
- NÃO instalar Bootstrap ou HTMX via npm — sempre via CDN (DA-24)
- CSRF: NÃO incluir token CSRF — desabilitado pois SameSite=Strict no cookie JWT protege (DA-26)

## Formatação obrigatória

Moeda (pt-BR):
  th:text="${#numbers.formatCurrency(valor)}"        → R$ 1.200,00

Data e hora:
  th:text="${#temporals.format(data, 'dd/MM/yyyy HH:mm')}"

CSRF: desabilitado — SameSite=Strict no cookie JWT protege (DA-26)
  NÃO incluir token CSRF nos forms

Autorização por perfil:
  sec:authorize="hasRole('CORRENTISTA')"
  sec:authorize="hasAnyRole('OPERADOR', 'ADMIN')"
  sec:authentication="name"

## Princípios SOLID (SRP)

- Um template por tela — sem template que faz duas coisas
- Fragmentos reutilizáveis em fragments/ — sem duplicação de navbar ou sidebar
- Controller popula o Model — template apenas exibe
