# Diagramas C4 — SIFAP 2.0

**Responsável:** Par 2 · Arquitetura (Enterprise Architect + Software Architect)
**Status:** Rascunho — aguardando validação no H2 com Par 3 e Par 4
**Data:** 20/05/2026

> Estes diagramas foram produzidos com base na leitura dos DDMs (BENEFICIARIO, PAGAMENTO,
> AUDITORIA, PROGRAMA-SOCIAL) e dos programas BATCHPGT.NSN e BATCHCON.NSN.
> O Par 1 (Visão) deve validar escopo antes da Passagem H2.

---

## Nível 1 — System Context (C4 L1)

Responsabilidade: Enterprise Architect
Pergunta respondida: "Quem usa o SIFAP e com quem ele se comunica?"

```mermaid
C4Context
    title SIFAP 2.0 — Contexto do Sistema

    Person(operador, "Operador MDAS", "Cadastra beneficiários, gerencia programas sociais e monitora pagamentos")
    Person(gestor, "Gestor de Programa", "Aprova ciclos, consulta relatórios, autoriza ajustes")
    Person(auditor, "Auditor TCU/CGU", "Consulta trilha de auditoria — somente leitura")

    System(sifap, "SIFAP 2.0", "Sistema de Fiscalização e Administração de Pagamentos. Gerencia beneficiários, calcula e emite pagamentos de programas sociais.")

    System_Ext(bb, "Banco do Brasil", "Recebe arquivo CNAB 240 com pagamentos. Retorna arquivo de confirmação/devolução.")
    System_Ext(siafi, "SIAFI", "Sistema de execução financeira. Recebe dotação orçamentária e confirma empenho de pagamentos.")
    System_Ext(cadUnico, "CadÚnico", "Base nacional de famílias em situação de vulnerabilidade. Fonte de dados de renda e composição familiar.")
    System_Ext(receitaFederal, "Receita Federal", "Valida CPF de beneficiários via consulta ao cadastro nacional.")

    Rel(operador, sifap, "Cadastra e gerencia beneficiários", "HTTPS/Web")
    Rel(gestor, sifap, "Aprova ciclos e consulta relatórios", "HTTPS/Web")
    Rel(auditor, sifap, "Consulta trilha de auditoria", "HTTPS/Web")
    Rel(sifap, bb, "Envia arquivo de pagamentos (D-1)", "CNAB 240 / SFTP")
    Rel(bb, sifap, "Retorna confirmação/devolução (D+1)", "CNAB 240 / SFTP")
    Rel(sifap, siafi, "Registra empenho orçamentário", "Webservice SOAP")
    Rel(sifap, cadUnico, "Consulta renda e composição familiar", "REST API")
    Rel(sifap, receitaFederal, "Valida CPF do beneficiário", "REST API")
```

### Integrações críticas identificadas no legado

| Sistema externo    | Programa legado     | Protocolo legado | Protocolo moderno proposto |
|--------------------|---------------------|------------------|---------------------------|
| Banco do Brasil    | BATCHCON.NSN        | CNAB 240 / SFTP  | CNAB 240 / SFTP (mantido) |
| SIAFI              | BATCHPGT.NSN (DDM)  | Webservice SOAP  | REST ou SOAP (ver ADR-002) |
| CadÚnico           | VALBENEF.NSN        | Batch file       | REST API                  |
| Receita Federal    | VALBENEF.NSN        | Módulo 11 local  | REST API + fallback local  |

> **Risco identificado (EA):** Integração com SIAFI via SOAP é contrato de 2002 (ver DDM PAGAMENTO campo integracao-siafi). Qualquer mudança exige janela de manutenção coordenada. Ver ADR-002.

---

## Nível 2 — Container Diagram (C4 L2)

Responsabilidade: Software Architect
Pergunta respondida: "Quais são os containers do SIFAP 2.0 e como se comunicam?"

```mermaid
C4Container
    title SIFAP 2.0 — Diagrama de Containers

    Person(operador, "Operador / Gestor", "Usuário interno MDAS")

    System_Boundary(sifap, "SIFAP 2.0") {
        Container(frontend, "Frontend Web", "Next.js 15 / TypeScript", "Interface do operador: cadastro, consulta, aprovação de ciclos, relatórios")
        Container(backend, "Backend API", "Java 21 / Spring Boot 3.3", "Módulo monolítico com 4 bounded contexts: beneficiary, payment, audit, admin")
        ContainerDb(db, "PostgreSQL 16", "PostgreSQL", "4 schemas isolados: beneficiary, payment, audit, admin")
        Container(batchRunner, "Batch Runner", "Spring Batch / Java 21", "Processa ciclo mensal (BATCHPGT), concilia retorno BB (BATCHCON), gera relatórios (BATCHREL)")
    }

    System_Ext(bb, "Banco do Brasil", "CNAB 240")
    System_Ext(siafi, "SIAFI", "SOAP")
    System_Ext(cadUnico, "CadÚnico", "REST")
    System_Ext(receitaFederal, "Receita Federal", "REST")

    Rel(operador, frontend, "Acessa via browser", "HTTPS")
    Rel(frontend, backend, "Chama API REST", "HTTPS / JSON")
    Rel(backend, db, "Lê e grava dados", "JDBC / JPA")
    Rel(batchRunner, db, "Lê beneficiários, grava pagamentos", "JDBC")
    Rel(batchRunner, bb, "Envia CNAB 240 e processa retorno", "SFTP")
    Rel(backend, siafi, "Registra empenho", "SOAP / HTTPS")
    Rel(backend, cadUnico, "Consulta composição familiar", "REST / HTTPS")
    Rel(backend, receitaFederal, "Valida CPF", "REST / HTTPS")
```

---

## Nível 3 — Component Diagram (C4 L3) — Bounded Context: Payment

Responsabilidade: Software Architect
Pergunta respondida: "Como o contexto Payment é organizado internamente?"

```mermaid
C4Component
    title SIFAP 2.0 — Componentes: Bounded Context Payment

    Container_Boundary(payment, "payment (módulo Spring)") {
        Component(payCtrl, "PaymentController", "Spring MVC @RestController", "Endpoints REST: /api/v1/payments — criação, consulta, cancelamento")
        Component(cycleService, "CycleService", "Spring @Service", "Orquestra geração mensal: chama calculadores, grava pagamentos, dispara auditoria")
        Component(calcService, "PaymentCalculatorService", "Spring @Service (domain)", "Calcula valor bruto, aplica descontos (teto 30% não-judicial), abono de dezembro")
        Component(discountService, "DiscountValidatorService", "Spring @Service (domain)", "Valida tipos e tetos de desconto por tipo: IR, JD (sem teto), CS, PA, TX")
        Component(payRepo, "PaymentRepository", "Spring Data JPA", "Persiste e consulta pagamentos no schema payment")
        Component(auditPublisher, "AuditEventPublisher", "Spring @Component", "Publica eventos de auditoria para o módulo audit via interface interna")
    }

    Container_Ext(batchRunner, "Batch Runner", "Spring Batch")
    Container_Ext(auditModule, "audit (módulo)", "Java")
    ContainerDb_Ext(db, "PostgreSQL — schema payment", "")

    Rel(batchRunner, cycleService, "Aciona geração mensal")
    Rel(payCtrl, cycleService, "Solicita geração / cancelamento")
    Rel(cycleService, calcService, "Delega cálculo por beneficiário")
    Rel(calcService, discountService, "Valida e aplica descontos")
    Rel(cycleService, payRepo, "Persiste pagamentos gerados")
    Rel(cycleService, auditPublisher, "Publica evento pós-geração")
    Rel(auditPublisher, auditModule, "Interface interna (sem import direto)")
    Rel(payRepo, db, "JDBC / JPA")
```

---

## Regras de Fronteira entre Módulos (SA)

> Estas regras serão validadas por ArchUnit no CI (ver ADR-001).

| Regra | Descrição |
|-------|-----------|
| R1 | Nenhum módulo importa classes do pacote `infrastructure` de outro módulo |
| R2 | Comunicação entre módulos ocorre exclusivamente via interfaces declaradas em `domain/` |
| R3 | Cada módulo possui schema PostgreSQL próprio e não faz JOIN em tabelas de outro schema |
| R4 | O módulo `audit` não recebe dependência direta de nenhum módulo — apenas publica eventos |
| R5 | O módulo `payment` referencia `beneficiary` somente via CPF (chave de negócio), nunca via entidade JPA |

---

## Bounded Contexts — Definição

| Contexto      | Origem no legado       | Responsabilidade no SIFAP 2.0                        | Entidades principais                |
|---------------|------------------------|------------------------------------------------------|-------------------------------------|
| `beneficiary` | DDM BENEFICIARIO       | Cadastro e validação de beneficiários                | Beneficiary, Dependent, Address     |
| `payment`     | DDM PAGAMENTO          | Ciclo de pagamento, descontos, conciliação bancária  | Payment, Cycle, Discount, BankReturn |
| `audit`       | DDM AUDITORIA          | Trilha imutável de todas as alterações (lei TCU)     | AuditEntry (append-only)            |
| `admin`       | DDM PROGRAMA-SOCIAL    | Parâmetros de programas sociais, regras de elegibilidade | SocialProgram, EligibilityRule   |

> **Nota SA:** o `FATOR-K` (campo BG do DDM PROGRAMA-SOCIAL, inserido em 2008 sem documentação)
> pertence ao contexto `admin` mas sua lógica de cálculo precisa ser investigada pelo Par 1 antes de
> especificar REQ-IDs. Marcar como mistério em `01-arqueologia/mysteries-found.md`.
