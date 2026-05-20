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
