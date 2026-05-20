# ADR-001 — Adotar Monolito Modular em vez de Microsserviços

**Data:** 20/05/2026
**Status:** Aceita
**Decisores:** Par 2 (EA + SA) · revisão Par 1 (PO)
**Origem no legado:** Estrutura dos 4 DDMs (BENEFICIARIO, PAGAMENTO, AUDITORIA, PROGRAMA-SOCIAL)

---

## Contexto

O SIFAP legado é um sistema monolítico Natural/Adabas com 4 bases de dados bem definidas e
fortemente acopladas pela auditoria — toda alteração em qualquer entidade gera registro em
AUDITORIA.ddm. O time tem 5 pessoas e 1 dia de trabalho para entregar protótipo funcional.

Restrições conhecidas:
- Time pequeno sem experiência prévia em infraestrutura de microsserviços.
- Auditoria transversal: quase toda operação em `beneficiary` e `payment` gera evento em `audit`.
  Comunicação cross-service via mensageria adicionaria latência e complexidade sem benefício real.
- Banco compartilhado no legado com 4 arquivos Adabas — separação de dados por contexto é viável
  via schemas PostgreSQL sem exigir bancos independentes.
- Janela de 1 dia não comporta service mesh, observabilidade distribuída e contratos versionados.

## Opções Consideradas

### Opção 1: Microsserviços (4 serviços independentes)
- **Descrição:** Um serviço por bounded context, deploy independente, comunicação via REST/mensageria.
- **Vantagens:** Escalabilidade independente por contexto, autonomia de deploy.
- **Desvantagens:** Exige service mesh, tracing distribuído, contratos versionados entre serviços.
  Com 5 pessoas em 1 dia, inviável entregar isso com qualidade. Auditoria transversal vira problema
  de consistência eventual.

### Opção 2: Monolito modular (escolhida)
- **Descrição:** Um único deployable Java com 4 módulos internos por bounded context.
  Comunicação inter-módulo via interfaces Java (sem chamada de rede). Um banco PostgreSQL com
  4 schemas isolados.
- **Vantagens:** Simplicidade de deploy, transações atômicas entre contextos quando necessário,
  auditoria simples e confiável, evolução para microsserviços possível no futuro via Strangler Fig.
- **Desvantagens:** Acoplamento em tempo de build — disciplina de fronteiras precisa ser imposta
  via ArchUnit no CI.

### Opção 3: Monolito sem modularização
- **Descrição:** Código organizado por camadas técnicas (controller/service/repository) sem fronteiras
  de domínio.
- **Vantagens:** Familiar, rápido de começar.
- **Desvantagens:** Reproduz o problema do legado. Fronteiras se erodem em semanas. Rejeitado.

## Decisão

**Adotaremos Monolito Modular** com Java 21 + Spring Boot 3.3, organizado por bounded context:

```
src/main/java/br/gov/sifap/
├── beneficiary/
│   ├── domain/         ← entidades e interfaces (sem Spring)
│   ├── application/    ← serviços que orquestram
│   └── infrastructure/ ← controllers, repositories, adapters
├── payment/
├── audit/
└── admin/
```

**Regras de fronteira (enforcement via ArchUnit no CI):**
1. Nenhum módulo importa classes de `infrastructure` de outro módulo.
2. Comunicação inter-módulo somente via interfaces em `domain/`.
3. Cada módulo usa apenas seu próprio schema PostgreSQL.
4. O módulo `audit` é append-only — sem UPDATE nem DELETE.

## Justificativa

O legado tem 4 domínios naturalmente definidos pelos seus DDMs. A auditoria transversal e o
time pequeno tornam microsserviços uma aposta ruim no prazo disponível. O Modular Monolith
preserva a clareza de fronteiras com custo operacional mínimo. A separação por schemas permite
evolução futura para microsserviços (Strangler Fig) sem reescrever a lógica de negócio.

## Consequências

- **Positivas:** Deploy simples, transações ACID disponíveis, auditoria síncrona e confiável.
- **Negativas:** Fronteiras precisam de disciplina ativa — ArchUnit é obrigatório no pipeline CI.
- **Quando revisitar:** Se o time crescer para > 3 squads autônomas ou se houver necessidade de
  escala diferenciada por contexto (ex.: `payment` com 10x mais carga que `admin`).
