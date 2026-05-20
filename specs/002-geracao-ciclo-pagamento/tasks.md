<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Tasks — 002 Geração de Ciclo de Pagamento

## Ordem de execução

### T01 — Testes de regra de desconto (antes do código)
- Criar testes de serviço para `REQ-PAY-003` e `REQ-PAY-004`.
- Cenários mínimos:
  - não judicial > 30% é truncado;
  - judicial sem teto;
  - mistura judicial + não judicial.
- Evidência: testes passam em `PaymentServiceTest`.

### T01b — Testes de fator K (antes do código)
- Criar testes para `REQ-ADM-004` em `ProgramServiceTest` e `PaymentCalculationTest`.
- Cenários mínimos:
  - FATOR-REAJ=0.10 + CONSTANTE_K=0.347215 → fator_k=1.0347215;
  - FATOR-REAJ=0 ou nulo → fator_k=1.00;
  - update de FATOR-REAJ gera `audit_event` PROGRAM_K_UPDATED;
  - leitura de CONSTANTE_K via `admin.factor_constants` (não hardcoded);
  - VLR-BASE-AJUSTADO usado em `PaymentCycleService` (integra com REQ-PAY-001).
- Evidência: testes verdes com Testcontainers + PostgreSQL.

### T02 — Testes de elegibilidade especial
- Criar testes para `REQ-PAY-002`.
- Cenários mínimos:
  - `regionCode=99` elegível automático;
  - região diferente de `99` aplica fluxo padrão.
- Evidência: testes passam em `EligibilityServiceTest`.

### T03 — Testes de validação cadastral
- Criar testes para `REQ-BEN-001`, `REQ-BEN-002`, `REQ-BEN-003`, `REQ-BEN-004`.
- Cenários mínimos:
  - status inválido;
  - CPF repetido e prefixo especial;
  - inclusão de dependente com status `C/D`;
  - limite de 5 dependentes.

### T04 — Implementar domínio de geração de ciclo
- Implementar `PaymentCycleService` para `REQ-PAY-001`.
- Persistir `CycleExecution` com métricas de gerados e rejeitados.
- **Pré-requisito:** T03b concluído (VLR-BASE-AJUSTADO disponível).

### T03b — Migration + domínio do fator K
- Criar migration Flyway `V<seq>__create_admin_factor_constants.sql`:
  - tabela `admin.factor_constants(key TEXT PK, value NUMERIC(7,6), updated_at TIMESTAMPTZ)`;
  - seed com `('CONSTANTE_K', 0.347215)` para paridade legada.
- Adicionar coluna `vlr_base_ajustado NUMERIC(15,2)` em `social_program`.
- Implementar `ProgramKCalculator` (`BigDecimal` + `RoundingMode.DOWN`) para `REQ-ADM-004`.
- Disparar `audit_event` PROGRAM_K_UPDATED em toda mudança de FATOR-REAJ.

### T05 — Implementar API REST
- Criar `POST /api/v1/payment-cycles` e `GET /api/v1/payment-cycles/{cycleId}` conforme `contracts/openapi.yaml`.
- Respostas esperadas: `201`, `200`, `404`, `409`.

### T06 — Implementar auditoria de execução
- Registrar `AuditEvent` para início/fim de ciclo e bloqueios de regra crítica.
- Cobrir logs sem expor dados sensíveis.

### T07 — Validar cenário de quickstart
- Executar passos de `quickstart.md`.
- Capturar evidência dos resultados esperados para cada REQ crítico.

## Critério de pronto
- Todos os testes de T01–T03 verdes.
- Endpoints operacionais e aderentes ao contrato.
- Fluxo manual validado com sucesso no quickstart.
