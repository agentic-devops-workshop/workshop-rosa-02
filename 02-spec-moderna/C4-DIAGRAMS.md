# C4 Diagrams — SIFAP 2.0

**Versão:** 0.1.0  
**Data:** 20/05/2026  
**Responsável:** Par 2 — Enterprise Architect + Software Architect

Este documento consolida a visão arquitetural do SIFAP 2.0 para a passagem H2.
Os diagramas abaixo refletem a spec vigente, os bounded contexts identificados no
Estágio 1 e as restrições operacionais do workshop.

## Premissas arquiteturais

- O sistema será entregue como um monolito modular.
- Os bounded contexts identificados são `beneficiary`, `payment`, `admin` e `audit`.
- O fluxo operacional crítico continua centrado no ciclo mensal de pagamento.
- A integração financeira legada com arquivos de remessa e retorno permanece uma restrição de desenho.
- A trilha de auditoria é requisito transversal e não pode ser tratada como detalhe opcional.

## Mapeamento de bounded contexts

| Contexto | Responsabilidade principal | Evidência legada principal |
|----------|-----------------------------|----------------------------|
| `beneficiary` | cadastro, documentação, status e dependentes | `BENEFICIARIO`, `CADBENEF.NSN`, `CADDEPEND.NSN`, `VALDOCS.NSN` |
| `payment` | geração de ciclo, cálculo, descontos, correção e conciliação | `PAGAMENTO`, `BATCHPGT.NSN`, `CALCBENF.NSN`, `CALCDSCT.NSN`, `CALCCORR.NSN`, `BATCHCON.NSN` |
| `admin` | cadastro e parametrização de programas sociais | `PROGRAMA-SOCIAL`, `CADPROG.NSN`, `VALELEG.NSN` |
| `audit` | registro e consulta de eventos de auditoria | `AUDITORIA`, `RELAUDIT.NSN` |

## Regras de fronteira

1. Um contexto não importa classes de infraestrutura de outro contexto.
2. Comunicação entre contextos ocorre por interfaces de aplicação ou eventos de domínio publicados internamente.
3. Cada contexto possui dono claro dos dados e seu próprio conjunto de tabelas no PostgreSQL.
4. O contexto `audit` é append-only e não recebe atualizações destrutivas.
5. O batch de pagamento pode consultar dados de `beneficiary` e `admin` por contratos explícitos, nunca por acesso direto a repositórios externos.

## C4 L1 — Contexto do sistema

```mermaid
C4Context
    title C4 L1 - Contexto do Sistema SIFAP 2.0

    Person(operator, "Operador", "Executa cadastro, consulta e rotinas operacionais")
    Person(finance, "Operação Financeira", "Acompanha lote, remessa e conciliação")
    Person(auditor, "Auditor", "Consulta trilha e evidências de conformidade")

    System(sifap, "SIFAP 2.0", "Modernização do SIFAP em monolito modular")

    System_Ext(bb, "Banco do Brasil", "Recebe remessa CNAB 240 e devolve retorno bancário")
    System_Ext(siafi, "SIAFI", "Sistema financeiro governamental para conciliação")
    System_Ext(receita, "Receita Federal", "Validação de CPF e consistência documental")

    Rel(operator, sifap, "Cadastra beneficiários, programas e consulta dados", "HTTPS")
    Rel(finance, sifap, "Gera ciclo, acompanha remessa e conciliação", "HTTPS")
    Rel(auditor, sifap, "Consulta auditoria e relatórios", "HTTPS")

    Rel(sifap, bb, "Envia remessa e processa retorno", "Arquivo CNAB 240")
    Rel(sifap, siafi, "Concilia ordens e confirmações", "Integração financeira legada")
    Rel(sifap, receita, "Valida dados documentais quando aplicável", "Integração externa")
```

## C4 L2 — Containers

```mermaid
C4Container
    title C4 L2 - Containers do SIFAP 2.0

    Person(user, "Usuário", "Operador, financeiro ou auditor")

    Container_Boundary(sifap, "SIFAP 2.0") {
        Container(frontend, "Portal Web", "Next.js 15", "Interface web para operação, consulta e auditoria")
        Container(api, "API de Domínio", "Java 21 + Spring Boot 3.3", "REST APIs e orquestração dos bounded contexts")
        Container(batch, "Batch de Pagamento", "Spring Batch", "Geração de ciclo, remessa e conciliação")
        ContainerDb(db, "PostgreSQL 16", "Relacional", "Schemas separados por bounded context")
    }

    System_Ext(bb, "Banco do Brasil", "CNAB 240")
    System_Ext(siafi, "SIAFI", "Conciliação financeira")

    Rel(user, frontend, "Usa via navegador", "HTTPS")
    Rel(frontend, api, "Consome", "REST/JSON")
    Rel(api, db, "Lê e grava", "JPA/Hibernate")
    Rel(batch, db, "Lê e grava em lote", "JDBC/JPA")
    Rel(batch, bb, "Envia remessa e recebe retorno", "Arquivo")
    Rel(batch, siafi, "Concilia ordens", "Integração financeira")
```

## C4 L3 — Componentes do contexto `payment`

```mermaid
C4Component
    title C4 L3 - Componentes do contexto Payment

    Container_Boundary(payment_api, "Payment Module") {
        Component(paymentController, "PaymentController", "REST Controller", "Expõe endpoints de ciclo e consulta")
        Component(cycleService, "CycleService", "Application Service", "Orquestra geração do ciclo mensal")
        Component(calculationService, "PaymentCalculationService", "Domain Service", "Calcula benefício, descontos e regras de dezembro")
        Component(deductionPolicy, "DeductionPolicy", "Domain Policy", "Aplica teto de 30% e exceção judicial")
        Component(remittanceGateway, "RemittanceGateway", "Outbound Port", "Publica remessa para banco pagador")
        Component(paymentRepository, "PaymentRepository", "Repository", "Persistência de pagamentos")
        Component(auditPublisher, "AuditEventPublisher", "Domain Event Publisher", "Publica eventos para o módulo audit")
    }

    Rel(paymentController, cycleService, "Invoca")
    Rel(cycleService, calculationService, "Usa")
    Rel(calculationService, deductionPolicy, "Usa")
    Rel(cycleService, paymentRepository, "Persiste")
    Rel(cycleService, remittanceGateway, "Solicita remessa")
    Rel(cycleService, auditPublisher, "Publica eventos")
```

## Observações para o handoff H2

- O Par 3 deve usar estes diagramas como referência de fronteira, não como licença para criar camadas genéricas sem dono de domínio.
- O contexto `payment` é o núcleo P0 e concentra o maior risco de regressão funcional.
- O contexto `admin` precisa preservar a semântica do FATOR-K definida em REQ-ADM-004.
- O contexto `audit` é transversal, mas não deve virar um atalho para acoplamento entre módulos.