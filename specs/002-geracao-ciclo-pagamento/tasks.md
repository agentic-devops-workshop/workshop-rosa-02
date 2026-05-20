<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Tasks — 002 Geração de Ciclo de Pagamento

## Ordem de execução

### T01 — Testes de regra de desconto (antes do código)
- Criar testes de serviço para `REQ-PAY-003` e `REQ-PAY-004`.
- Cenários mínimos:
  - cap = 30% × VLR-BRUTO (bruto integral, não bruto-judicial);
  - não judicial > 30% é truncado;
  - judicial puro sem teto;
  - **ordem judicial → não-judicial**: J=500 + I=100 em bruto=1000 → total=300 (cap em I trunca acumulado total);
  - **ordem não-judicial → judicial**: I=200 + J=500 em bruto=1000 → total=700 (cap não dispara em I=200; J livre);
  - paridade de ordenação do PE-GROUP por (DT-INICIO-DSCT, ordem-de-inserção).
- Evidência: testes passam em `PaymentServiceTest` com snapshot dos 4 cenários ordem-dependentes.

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
  - tabela `admin.factor_constants(key TEXT PK, value NUMERIC(7,6), min_range NUMERIC(7,6), max_range NUMERIC(7,6), updated_at TIMESTAMPTZ)`;
  - seed com `('CONSTANTE_K', 0.347215, 0.10, 0.99)` para paridade legada.
- Adicionar coluna `vlr_base_ajustado NUMERIC(15,2)` e `factor_k NUMERIC(7,6)` em `social_program`.
- Implementar `ProgramKCalculator` (`BigDecimal` + `RoundingMode.DOWN`) para `REQ-ADM-004`.
- Disparar `audit_event` PROGRAM_K_UPDATED em toda mudança de FATOR-REAJ.

### T03c — Migration de payment_discount (split 1:N)
- Criar migration Flyway `V<seq>__create_payment_discount.sql`:
  - tabela `payment_discount(id UUID PK, payment_id UUID FK, discount_type CHAR(1), amount NUMERIC(15,2), percentage NUMERIC(5,2) NULL, start_date DATE, end_date DATE NULL, processing_order INT, cap_applied BOOLEAN DEFAULT false, UNIQUE(payment_id, processing_order))`;
  - CHECK `discount_type IN ('J','P','I','S','A','T','E','O')`.
- Em `payment`: substituir `non_judicial_discount_amount` e `judicial_discount_amount` por `total_discount_amount` (mantido por agregador).
- Migrar `competence` de `string(6)` para `DATE` (1º dia do mês).
- Implementar serializer/deserializer Jackson convertendo `YearMonth` ↔ `YYYY-MM` na fronteira REST.

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
