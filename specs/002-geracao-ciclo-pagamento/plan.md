<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Plan — 002 Geração de Ciclo de Pagamento

## Objetivo
Implementar geração mensal de pagamentos com fidelidade às regras legadas mapeadas na `spec.md`, priorizando `REQ-PAY-001` a `REQ-PAY-004` e validações de beneficiário.

## Escopo Técnico
- Backend Java 21 + Spring Boot 3.3.
- Persistência em PostgreSQL (`beneficiary`, `payment`, `social_program`, `audit_event`).
- Endpoints REST versionados em `/api/v1`.
- Auditoria para eventos de geração de ciclo e rejeições de elegibilidade.

## Módulos Envolvidos (Monólito Modular)
- `payment`: geração de ciclo, cálculo de desconto e total líquido.
- `beneficiary`: validação de status, CPF e dependentes.
- `eligibility`: aplicação de regras por região/programa.
- `audit`: trilha de eventos de ciclo e erros de regra.

## Estratégia de Implementação
1. Escrever testes de serviço para regras críticas (`REQ-PAY-*`, `REQ-BEN-*`).
2. Implementar serviços de domínio com regras do legado.
3. Expor API de geração de ciclo e consulta de execução.
4. Persistir evento de auditoria para ciclo e bloqueios de regra.
5. Validar fluxo manual via `quickstart.md`.

## Riscos e Mitigação
- Risco: divergência em regras de desconto judicial.
  - Mitigação: testes parametrizados com cenários mistos (`J` + não `J`).
- Risco: falsa elegibilidade por regra especial de região 99.
  - Mitigação: teste dedicado para `COD-REGIAO=99` com trilha de auditoria.
- Risco: regressão por CPF/prefixos especiais.
  - Mitigação: tabela de casos com CPF comum e exceção legada.

## Dependências
- Descobertas de `01-arqueologia/business-rules-catalog.md`.
- Priorização de `01-arqueologia/discovery-report.md`.
- Mapeamento de domínio em `specs/002-geracao-ciclo-pagamento/data-model.md`.
