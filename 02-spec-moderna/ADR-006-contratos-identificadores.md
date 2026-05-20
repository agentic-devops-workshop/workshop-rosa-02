<!-- markdownlint-disable MD013 MD025 MD040 -->

# ADR-006 — Contratos e identificadores da API de pagamento

- **Status:** Aceita (decisão arquitetural unilateral do Software Architect, dentro do mandato do Par 2 sobre contratos REST e modelo de dados)
- **Data:** 20/05/2026
- **Decisor:** Par 2 — Software Architect
- **Origem:** `/speckit.analyze` da [spec 002](../specs/002-geracao-ciclo-pagamento/spec.md), bloqueador **B4** (UUID×Long, AAAAMM×YYYY-MM, path de endpoint, split de descontos)
- **Relacionada:** [ADR-001](ADR-001-monolito-modular.md), [ADR-003](ADR-003-auditoria-imutavel.md), [ADR-004](ADR-004-parametrizacao-constantes-financeiras.md), [ADR-005](ADR-005-paridade-vs-determinismo-descontos.md)

## Contexto

O `/speckit.analyze` revelou divergência tripla entre [contracts/openapi.yaml](../specs/002-geracao-ciclo-pagamento/contracts/openapi.yaml), [data-model.md](../specs/002-geracao-ciclo-pagamento/data-model.md) e o protótipo em [src/main/java/com/sifap/payment/](../src/main/java/com/sifap/payment/). Para destravar o início do Estágio 3, o SA precisa fixar quatro decisões de contrato:

| # | Decisão | Hoje |
|---|---|---|
| D-A | Identificador de entidades de domínio | `Long` no protótipo, `UUID` no data-model e OpenAPI |
| D-B | Formato da competência mensal | `cycle: String "YYYY-MM"` no protótipo, `competence: string ^\\d{6}$` no OpenAPI, `string(6) AAAAMM` no data-model |
| D-C | Path do endpoint de ciclo | `/api/v1/payments/cycles` no protótipo, `/api/v1/payment-cycles` no OpenAPI |
| D-D | Modelo de descontos no `Payment` | `totalDiscount` único no protótipo; `judicial` + `nonJudicial` + `total` no data-model; nenhum no OpenAPI |

Estas são decisões **arquiteturais** (não de negócio): formato técnico, nomenclatura REST, modelagem de persistência. O Software Architect pode decidir unilateralmente desde que a decisão fique registrada e respeite [ADR-001](ADR-001-monolito-modular.md), [REQ-PAY-001/002](SPECIFICATION.md) e [REQ-PAY-006](SPECIFICATION.md) (introduzido por [ADR-005](ADR-005-paridade-vs-determinismo-descontos.md)).

## Decisões

### D-A · Identificador: **UUID v7 para todas as entidades novas**

- **Tipo Java:** `java.util.UUID`.
- **Tipo PostgreSQL:** `uuid` nativo (extensão `pgcrypto` para `gen_random_uuid()` na migration; geração preferencial **na aplicação** com `UUID.randomUUID()` ou `UuidCreator.getTimeOrderedEpoch()` para v7 quando disponível).
- **Geração JPA:** sem `@GeneratedValue` automático; PK atribuída no construtor da entidade no momento de criação. Evita round-trip ao banco para descobrir o ID.
- **Exposição na API:** sempre como string canônica em minúsculas (`8a47ec19-3a0c-4f2c-9c74-2c6a8f4f4a91`).
- **Migrations:** todas as tabelas novas usam `uuid PRIMARY KEY`. Tabelas que já existiam com `BIGSERIAL` (caso do protótipo atual) serão recriadas — o protótipo não tem dados a preservar.

### D-B · Competência: **`yearMonth: string ISO 8601 `YYYY-MM`**

- **Nome do campo:** `yearMonth` em DTOs e contrato; `year_month` em snake_case no banco; getter `getYearMonth()` na entidade.
- **Tipo Java:** `java.time.YearMonth` na camada de domínio e aplicação. Conversão automática Spring para `YearMonth.parse(...)`.
- **Tipo PostgreSQL:** `CHAR(7)` com `CHECK (year_month ~ '^[0-9]{4}-(0[1-9]|1[0-2])$')`. Não usar `DATE` truncado nem dois inteiros (`year`, `month`).
- **OpenAPI:** `type: string, pattern: '^[0-9]{4}-(0[1-9]|1[0-2])$', example: '2026-05'`.
- **Renome:** o campo `cycle` no protótipo passa a `yearMonth`. O termo "cycle" passa a designar a **execução** (`CycleExecution`), não a competência mensal.
- **Justificativa:** ISO 8601 é interoperável; `YearMonth` é tipo nativo Java; `AAAAMM` é representação interna do legado, não deve vazar para API moderna.

### D-C · Path do endpoint de ciclo: **`/api/v1/payment-cycles`**

- **Padrão REST:** recurso é o substantivo plural; ações são verbos HTTP. `payment-cycles` é o recurso, não `payments/cycles`.
- **Endpoints normativos:**

  | Método | Path | Operação |
  |---|---|---|
  | `POST` | `/api/v1/payment-cycles` | Inicia execução de ciclo (assíncrono ou síncrono curto) |
  | `GET` | `/api/v1/payment-cycles/{cycleId}` | Consulta status/métricas do ciclo |
  | `GET` | `/api/v1/payment-cycles?yearMonth=YYYY-MM` | Lista ciclos por competência |
  | `GET` | `/api/v1/payments?cycleId={id}` | Lista pagamentos de um ciclo (ordenados por CPF — REQ-PAY-005) |
  | `POST` | `/api/v1/payments/{paymentId}/discounts` | Aplica descontos (REQ-PAY-001/002/006) |
  | `POST` | `/api/v1/payments/{paymentId}/corrections` | Correção retroativa (REQ-PAY-004) |
- **Renome no protótipo:** `/api/v1/payments/cycles` → `/api/v1/payment-cycles`. Endpoints de aplicação de descontos permanecem aninhados em `/api/v1/payments/{id}/...` porque são ações sobre o pagamento individual, não sobre o ciclo.

### D-D · Split de descontos no `Payment`: **três campos separados**

- **Campos persistidos:**

  | Campo | Tipo | Descrição |
  |---|---|---|
  | `judicialDiscountAmount` | `BigDecimal(14,2)` | Soma de descontos judiciais. Aplicado integralmente (REQ-PAY-002). |
  | `nonJudicialDiscountAmount` | `BigDecimal(14,2)` | Soma de descontos não-judiciais **após** aplicação do cap de 30 % (REQ-PAY-001 / REQ-PAY-006). |
  | `totalDiscountAmount` | `BigDecimal(14,2)` (calculado, não persistido) | `judicialDiscountAmount + nonJudicialDiscountAmount`. |
- **Histórico individual:** entidade nova `PaymentDiscount(id, paymentId, type, requestedAmount, appliedAmount, source, createdAt)` em `payment.infrastructure.persistence`. Persiste **toda** linha de desconto (mesmo as truncadas pelo cap), com auditoria completa.
- **Justificativa:**
  - Compliance: relatórios judiciais (CNJ, ofícios) exigem decompor judicial vs. não-judicial. Sem split, exigência não pode ser atendida.
  - REQ-PAY-006 (ADR-005) torna o cap aplicável apenas ao subtotal não-judicial — separar os campos torna a regra **expressa no schema**, não escondida em lógica de service.
  - Auditoria (REQ-AUD-001): `PaymentDiscount.requestedAmount` × `appliedAmount` registra clamp aplicado, satisfazendo trilha de "estado anterior → posterior".

## Consequências

### Positivas

- API e dados ficam coerentes com OpenAPI e data-model que já estavam descritos para a spec 002.
- Compliance financeiro/judicial passa a ser atendível por construção, não por relatório derivado.
- `YearMonth` Java nativo elimina parsing manual de `AAAAMM` espalhado.
- UUID elimina enumeração de IDs por terceiros e facilita migração entre ambientes (dev/stage/prod sem colisão).
- Split de descontos torna ADR-005 verificável diretamente nos dados.

### Negativas

- **Refactor obrigatório no protótipo** antes de qualquer feature nova. Mitigação: PR único e estrutural, sem mudança de comportamento; testes existentes precisam continuar verdes.
- UUID gera índices maiores (16 B vs. 8 B). Mitigação: para `payment` o índice crítico é por (`yearMonth`, `beneficiaryCpf`), não por PK; impacto desprezível.
- Renomear `cycle` → `yearMonth` invalida testes que usam literais; é trabalho mecânico bem coberto por compilador.

### Riscos

- **R1 — UUID v7 nem sempre está disponível.** Mitigação: começar com `UUID.randomUUID()` (v4); migrar para v7 (`com.github.f4b6a3:uuid-creator`) só quando ordenação por inserção virar requisito explícito.
- **R2 — Drift entre `totalDiscountAmount` calculado e somatório real.** Mitigação: trigger no banco recalculando + teste unitário garantindo invariante.

## Plano de adoção

| # | Ação | Owner | Quando |
|---|---|---|---|
| 1 | PR estrutural: `com.sifap` → `br.gov.sifap` + split em `domain/application/infrastructure` (ver [H2-RESPOSTAS-PAR2.md](H2-RESPOSTAS-PAR2.md) Q1) | Par 3 | Sprint 1 |
| 2 | Refactor `Payment.id`: `Long` → `UUID`; remover `@GeneratedValue` | Par 3 + Par 4 (DBA) | Sprint 1 |
| 3 | Renome `cycle` → `yearMonth`; tipo `YearMonth` no domínio; coluna `year_month CHAR(7)` no banco | Par 3 + Par 4 | Sprint 1 |
| 4 | Renome do endpoint `/payments/cycles` → `/payment-cycles`; ajustar `PaymentController` e testes | Par 3 | Sprint 1 |
| 5 | Quebrar `totalDiscount` em `judicialDiscountAmount` + `nonJudicialDiscountAmount`; criar entidade `PaymentDiscount` | Par 3 + Par 4 | Sprint 1 |
| 6 | Atualizar OpenAPI da spec 002 com schemas atualizados + Problem Details RFC 7807 | Par 2 (este PR) | Imediato |
| 7 | Atualizar [data-model.md](../specs/002-geracao-ciclo-pagamento/data-model.md) e [plan.md](../specs/002-geracao-ciclo-pagamento/plan.md) com decisões | Par 1 (RE) | Mesmo dia |
| 8 | Atualizar [docs/codemap-payment.md](../docs/codemap-payment.md) com smells fechados | Par 2 | Imediato |

## Por que esta ADR não precisa do PO

ADR-006 não muda **comportamento de negócio** — não muda quem recebe quanto, em qual data, com qual desconto. Muda **forma** dos dados (UUID, YYYY-MM), **nomenclatura** (path REST), **decomposição** (split de descontos). Decisões arquiteturais clássicas, dentro do mandato do Par 2 conforme [05-personas/04-software-architect/PERSONA.md](../05-personas/04-software-architect/PERSONA.md): "Dono da estrutura interna do sistema. Decide como módulos são organizados, quais abstrações são expostas e quais ficam privadas".

ADR-004 (FATOR-K) e ADR-005 (determinismo) **mudam comportamento financeiro observável** e por isso precisam de aceite do PO. ADR-006 não.

## Status do diário arquitetural após esta ADR

| ADR | Status |
|---|---|
| [ADR-001](ADR-001-monolito-modular.md) — Modular monolith | Aceita |
| [ADR-002](ADR-002-estrategia-integracao.md) — Integração BB/SIAFI | Aceita |
| [ADR-003](ADR-003-auditoria-imutavel.md) — Auditoria append-only | Aceita |
| [ADR-004](ADR-004-parametrizacao-constantes-financeiras.md) — Parametrização de constantes financeiras | Proposta (aguarda PO + EA) |
| [ADR-005](ADR-005-paridade-vs-determinismo-descontos.md) — Determinismo do cálculo de descontos | Proposta (aguarda PO + EA) |
| ADR-006 — Contratos e identificadores (este) | **Aceita** |
