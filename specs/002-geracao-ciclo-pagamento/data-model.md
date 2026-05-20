<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Data Model — 002 Geração de Ciclo de Pagamento

## Entidades

### Beneficiary
- `id`: UUID
- `cpf`: string(11)
- `status`: enum(`A`,`S`,`C`,`I`,`D`)
- `regionCode`: string
- `birthDate`: date
- `dependentsCount`: integer
- `programCode`: string

### Payment
- `id`: UUID
- `beneficiaryId`: UUID (FK)
- `competence`: string(6) `AAAAMM`
- `grossAmount`: decimal(15,2)
- `nonJudicialDiscountAmount`: decimal(15,2)
- `judicialDiscountAmount`: decimal(15,2)
- `totalDiscountAmount`: decimal(15,2)
- `netAmount`: decimal(15,2)
- `status`: enum(`GENERATED`,`PAID`,`RETURNED`,`REVERSED`)

### CycleExecution
- `id`: UUID
- `competence`: string(6)
- `startedAt`: timestamp
- `finishedAt`: timestamp
- `generatedPayments`: integer
- `rejectedBeneficiaries`: integer
- `status`: enum(`RUNNING`,`COMPLETED`,`FAILED`)

### AuditEvent
- `id`: UUID
- `eventType`: string
- `action`: string
- `referenceKey`: string
- `payload`: jsonb
- `createdAt`: timestamp

## Regras de Integridade
- `Beneficiary.status` aceita apenas `A,S,C,I,D` (REQ-BEN-001).
- `Payment.nonJudicialDiscountAmount <= grossAmount * 0.30` (REQ-PAY-003).
- `Payment.totalDiscountAmount = nonJudicialDiscountAmount + judicialDiscountAmount` (REQ-PAY-004).
- Para `regionCode=99`, elegibilidade é direta (REQ-PAY-002).

## Relações
- `Beneficiary 1:N Payment`
- `CycleExecution 1:N Payment` (via campo de competência + referência de execução)
- `CycleExecution 1:N AuditEvent`
