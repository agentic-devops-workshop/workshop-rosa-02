<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Data Model — 002 Geração de Ciclo de Pagamento

## Decisões de modelagem (RE · Par 1, 20/05/2026)

| Tema | Decisão | Justificativa |
|------|---------|---------------|
| **Tipo de chave primária** | `UUID` (v7 ordenado por tempo) em todas as tabelas | Não-enumerável; seguro para exposição em API pública; legado usa ISN Adabas que não é exposto |
| **Competência (mês de referência)** | `YearMonth` Java + `DATE` PostgreSQL (1º dia do mês) internamente; **`YYYY-MM` ISO 8601** em toda API JSON | Legado usa `AAAAMM` (PAGAMENTO.ddm campo AE, numérico 6); conversão ocorre na fronteira controller. Evita ambiguidade Set/26 vs 09/26. |
| **Path do endpoint** | `/api/v1/payment-cycles` (plural, kebab-case) | Aderente à convenção do projeto (REST + versionamento) |
| **Descontos** | Tabela `payment_discount` 1:N (não colunas agregadas) com `processing_order` estável | Legado usa GRP-DESCONTO (PE até 8 itens). REQ-PAY-002 exige iteração item-a-item para preservar efeito ordem-dependente. |

## Entidades

### Beneficiary
- `id`: UUID (PK)
- `cpf`: string(11) UNIQUE
- `status`: enum(`A`,`S`,`C`,`I`,`D`)
- `region_code`: string(2)
- `birth_date`: DATE
- `dependents_count`: integer (0..5, REQ-BEN-004)
- `program_code`: string FK → `social_program.code`

### SocialProgram
- `id`: UUID (PK)
- `code`: string(4) UNIQUE
- `name`: string
- `factor_reaj`: NUMERIC(5,4) NULL
- `vlr_base`: NUMERIC(15,2)
- `vlr_base_ajustado`: NUMERIC(15,2) (calculado por REQ-ADM-004)
- `factor_k`: NUMERIC(7,6) (= 1 + factor_reaj × CONSTANTE_K)
- `status`: enum(`ACTIVE`,`INACTIVE`)

### Payment
- `id`: UUID (PK)
- `beneficiary_id`: UUID (FK)
- `cycle_execution_id`: UUID (FK)
- `competence`: DATE (1º dia do mês; serializado como `YYYY-MM` na API)
- `gross_amount`: NUMERIC(15,2) NOT NULL
- `total_discount_amount`: NUMERIC(15,2) NOT NULL DEFAULT 0
- `net_amount`: NUMERIC(15,2) GENERATED (= gross_amount - total_discount_amount)
- `status`: enum(`GENERATED`,`PAID`,`RETURNED`,`REVERSED`)
- `created_at`: TIMESTAMPTZ

### PaymentDiscount (NOVO — split 1:N)
- `id`: UUID (PK)
- `payment_id`: UUID (FK → `payment.id`, ON DELETE CASCADE)
- `discount_type`: enum(`J`,`P`,`I`,`S`,`A`,`T`,`E`,`O`) (legado: JUDICIAL/PENSAO/IMPOSTO/SINDICAL/ADMINISTRATIVO/TAXA/EMPRESTIMO/OUTROS)
- `amount`: NUMERIC(15,2) NOT NULL
- `percentage`: NUMERIC(5,2) NULL
- `start_date`: DATE
- `end_date`: DATE NULL
- `processing_order`: integer NOT NULL (preserva ordem PE-GROUP do legado, REQ-PAY-002)
- `cap_applied`: boolean DEFAULT false (true quando truncamento de teto disparou neste item)
- UNIQUE(`payment_id`, `processing_order`)

### Estratégia de índice para escala
- Confirmar com Par 2 (Architect) e Par 4 (DBA) a estratégia de índice de `payment_discount` para carga alvo de 3.8M registros/mês.
- Índice mínimo obrigatório: `payment_discount(payment_id, processing_order)` (o `UNIQUE` já deve atender este padrão de acesso).
- Validar plano de execução para leitura de descontos por pagamento em ordem estável de processamento.

### CycleExecution
- `id`: UUID (PK)
- `competence`: DATE UNIQUE
- `started_at`: TIMESTAMPTZ
- `finished_at`: TIMESTAMPTZ NULL
- `generated_payments`: integer
- `rejected_beneficiaries`: integer
- `status`: enum(`RUNNING`,`COMPLETED`,`FAILED`)

### AuditEvent
- `id`: UUID (PK)
- `event_type`: string (ex.: `CYCLE_GENERATED`, `PROGRAM_K_UPDATED`, `DISCOUNT_CAPPED`)
- `action`: enum(`IN`,`AL`,`EX`)
- `reference_key`: string
- `payload`: jsonb
- `created_at`: TIMESTAMPTZ
- Restrição: `UPDATE`/`DELETE` retornam 403 (REQ-AUD-001)

### FactorConstant (suporte a REQ-ADM-004)
- `key`: string PK (ex.: `CONSTANTE_K`)
- `value`: NUMERIC(7,6)
- `min_range`: NUMERIC(7,6)
- `max_range`: NUMERIC(7,6)
- `updated_at`: TIMESTAMPTZ

## Regras de Integridade

- `Beneficiary.status` aceita apenas `A,S,C,I,D` (REQ-BEN-001)
- `Beneficiary.dependents_count <= 5` (REQ-BEN-004)
- `Payment.total_discount_amount = SUM(payment_discount.amount WHERE payment_id = payment.id)` (mantido por trigger ou recalculado a cada inserção de PaymentDiscount)
- Para `region_code='99'`, elegibilidade é direta com bypass de programa/renda/idade (REQ-PAY-002)
- `payment_discount.processing_order` é contíguo a partir de 1 e preserva ordem (DT-INICIO-DSCT, ordem-de-inserção) do legado
- `social_program.factor_k = 1 + (factor_reaj × factor_constants.value WHERE key='CONSTANTE_K')` (REQ-ADM-004)

## Relações

- `SocialProgram 1:N Beneficiary`
- `Beneficiary 1:N Payment`
- `CycleExecution 1:N Payment`
- `Payment 1:N PaymentDiscount` (NOVO — substitui colunas agregadas)
- `CycleExecution 1:N AuditEvent`

## Mapeamento legado → moderno

| Legado (Adabas/DDM) | Moderno (PostgreSQL) | Observação |
|---------------------|----------------------|-------------|
| `PAGAMENTO.AE ANO-MES-REF` (N6, AAAAMM) | `payment.competence` (DATE) | Conversão `YYYYMM` → `DATE(YYYY, MM, 1)`; serializa como `YYYY-MM` na API |
| `PAGAMENTO.GRP-DESCONTO` (PE max 8) | `payment_discount` (1:N) | Preserva ordem via `processing_order`; libera limite de 8 itens |
| `PAGAMENTO.CB TIPO-DESCONTO` (A3) | `payment_discount.discount_type` (enum) | Mapeamento: J→J, P→P, I→I, S→S, A→A, T→T, E→E, O→O |
| `PROGRAMA-SOCIAL.BG FATOR-K` | `social_program.factor_k` | Calculado por REQ-ADM-004; persistido para auditoria |
| ISN Adabas (interno) | UUID v7 | Chave externa segura para API |
