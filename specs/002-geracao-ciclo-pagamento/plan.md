<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Plan — 002 Geração de Ciclo de Pagamento

## Decisões arquiteturais que governam este plano

| ADR | Decisão | Aplica a |
|---|---|---|
| [ADR-001](../../02-spec-moderna/ADR-001-monolito-modular.md) | Modular monolith por bounded context (`beneficiary`, `payment`, `admin`, `audit`); camadas `domain/application/infrastructure` por contexto | Toda a estrutura de pacotes |
| [ADR-003](../../02-spec-moderna/ADR-003-auditoria-imutavel.md) | Auditoria append-only — schema, JPA `@Immutable`, `Propagation.REQUIRES_NEW` no service | Estratégia 4 |
| [ADR-004](../../02-spec-moderna/ADR-004-parametrizacao-constantes-financeiras.md) | `SifapFinancialProperties` parametriza FATOR-K, cap não-judicial e abono natalino — proibido hardcode | REQ-PAY-001/003, REQ-ADM-004 |
| [ADR-005](../../02-spec-moderna/ADR-005-paridade-vs-determinismo-descontos.md) | Cálculo de descontos determinístico (cap apenas sobre subtotal não-judicial); divergência intencional do legado registrada como `REQ-PAY-006` | Estratégia 2 |
| [ADR-006](../../02-spec-moderna/ADR-006-contratos-identificadores.md) | UUID v4 para PKs; competência `yearMonth: "YYYY-MM"`; path `/api/v1/payment-cycles`; split `judicial/nonJudicial/total` em `Payment`; entidade `PaymentDiscount` para histórico | Toda a API e schema |
| [H2-RESPOSTAS-PAR2](../../02-spec-moderna/H2-RESPOSTAS-PAR2.md) | Respostas Q1–Q4 do handoff (fronteiras, audit imutável, ordenação CPF, FATOR-K) | Sequência de PRs |

## Objetivo
Implementar geração mensal de pagamentos com fidelidade às regras legadas mapeadas na `spec.md` e nas ADRs acima, priorizando `REQ-PAY-001` a `REQ-PAY-006` e validações de beneficiário.

## Escopo Técnico
- Backend Java 21 + Spring Boot 3.3, package raiz `br.gov.sifap` (ADR-001).
- Persistência em PostgreSQL 16 com **Flyway**: tabelas `beneficiary`, `payment`, `payment_discount`, `cycle_execution`, `social_program`, `audit_event`.
- Identificadores `uuid` em todas as tabelas novas (ADR-006 § D-A).
- Endpoints REST versionados em `/api/v1` com Problem Details (RFC 7807) — ver [contracts/openapi.yaml](contracts/openapi.yaml).
- Autenticação JWT bearer (REQ-SEC-001).
- Auditoria append-only com trigger PostgreSQL e `REQUIRES_NEW` (ADR-003) para eventos de geração de ciclo, aplicação de descontos, correção retroativa e rejeições de elegibilidade.

## Módulos Envolvidos (Monólito Modular)

Layout obrigatório por contexto (ADR-001 + [H2-RESPOSTAS-PAR2.md](../../02-spec-moderna/H2-RESPOSTAS-PAR2.md) Q1):

```text
br/gov/sifap/<context>/
  domain/          ← agregados puros, sem JPA
  application/
    port/          ← interfaces consumidas por outros contextos / pela infra
    service/       ← @Transactional, casos de uso
    dto/           ← request/response da camada de aplicação
  infrastructure/
    persistence/   ← @Entity JPA, Spring Data
    rest/          ← @RestController, exception handlers
    integration/   ← gateways externos e adapters de outros contextos
```

- `payment`: geração de ciclo, cálculo determinístico de descontos (ADR-005), 13º + abono (REQ-PAY-003), correção retroativa.
- `beneficiary`: validação de status, CPF e dependentes — exposto a `payment` por `BeneficiaryQueryPort`.
- `admin`: programas sociais, FATOR-K (REQ-ADM-004) — exposto a `payment` por `SocialProgramQueryPort`.
- `audit`: trilha append-only consumida por todos via `AuditService` (`REQUIRES_NEW`).

Enforcement: ArchUnit no `mvn verify` rejeitando dependência cruzada fora de `application.port`.

## Estratégia de Implementação
1. **PR estrutural** (sem mudança de comportamento): rename `com.sifap` → `br.gov.sifap`, split em `domain/application/infrastructure`, introdução de ArchUnit, refactor `Long` → `UUID`, renome `cycle` → `yearMonth`, renome path `/payments/cycles` → `/payment-cycles` (ADR-006).
2. **Configuração financeira:** classe `SifapFinancialProperties` consumindo `sifap.financial.*` (ADR-004); `PaymentService` e `SocialProgramService` recebem o bean.
3. **Flyway baseline:** migration inicial criando `beneficiary`, `payment`, `payment_discount`, `cycle_execution`, `social_program`, `audit_event` (UUID PK, `year_month CHAR(7)`, índice parcial `idx_beneficiary_active_cpf`, triggers de imutabilidade em `audit_event`).
4. **Testes determinísticos primeiro:** property-based test para `applyDiscounts` cobrindo permutações de ordem (ADR-005), parametrização do FATOR-K (ADR-004), regressão contra vetor legado.
5. **Domínio de geração de ciclo:** `PaymentCycleService` para REQ-PAY-005 com keyset pagination + chunks (H2 Q3); persiste `CycleExecution` com métricas e `last_processed_cpf`.
6. **API REST:** `POST /api/v1/payment-cycles`, `GET /api/v1/payment-cycles/{cycleId}`, `GET /api/v1/payments?cycleId=…`, `POST /api/v1/payments/{id}/discounts`, `POST /api/v1/payments/{id}/corrections` — todos com Problem Details RFC 7807.
7. **Auditoria:** `AuditService.record(...)` com `Propagation.REQUIRES_NEW` ou `@TransactionalEventListener(AFTER_COMMIT)`; CPF mascarado obrigatório.
8. **Validação manual:** roteiro em [quickstart.md](quickstart.md).

## Riscos e Mitigação
- Risco: divergência em regras de desconto judicial (ordem-dependência do legado).
  - Mitigação: `REQ-PAY-006` + ADR-005 + property-based test contra permutações.
- Risco: cálculo errado por uso de `VLR-BASE` bruto (FATOR-K).
  - Mitigação: ADR-004 + teste de regressão com vetor legado em `cadprog-fixtures.csv`.
- Risco: falsa elegibilidade por regra especial de região 99.
  - Mitigação: teste dedicado para `regionCode=99` com trilha de auditoria.
- Risco: regressão por CPF/prefixos especiais.
  - Mitigação: tabela de casos com CPF comum e exceção legada.
- Risco: heap explode ao carregar 2,3 M beneficiários ativos no batch.
  - Mitigação: keyset pagination + índice parcial (H2 Q3); sem `findAll()` em domínio quente.
- Risco: auditoria perdida em rollback de domínio.
  - Mitigação: `Propagation.REQUIRES_NEW` ou listener pós-commit (H2 Q2).

## Dependências
- Descobertas de [`01-arqueologia/business-rules-catalog.md`](../../01-arqueologia/business-rules-catalog.md).
- Priorização de [`01-arqueologia/discovery-report.md`](../../01-arqueologia/discovery-report.md).
- Mapeamento de domínio em [`data-model.md`](data-model.md).
- ADRs 001 / 003 / 004 / 005 / 006 (tabela acima).
- Mapa de código atualizado em [`docs/codemap-payment.md`](../../docs/codemap-payment.md).
