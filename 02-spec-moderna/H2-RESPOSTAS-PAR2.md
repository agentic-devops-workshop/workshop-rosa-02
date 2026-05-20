<!-- markdownlint-disable MD013 MD025 MD040 -->

# Respostas H2 — Par 2 → Par 3

> **Origem das perguntas:** [02-spec-moderna/H2-HANDOFF-PAR3.md § Perguntas que o Par 3 deve responder cedo](H2-HANDOFF-PAR3.md)
> **Autor:** Par 2 · Software Architect (com referendo do Enterprise Architect)
> **Data:** 20/05/2026
> **Status:** Resposta consolidada para o início do Estágio 3

Este documento entrega ao Par 3 o **caminho recomendado** para as quatro perguntas que ficaram em aberto na passagem. Cada resposta é prescritiva no nível de arquitetura e deixa decisões de implementação para o Tech Lead, observando os limites do [ADR-001](ADR-001-monolito-modular.md), [ADR-003](ADR-003-auditoria-imutavel.md) e [ADR-004](ADR-004-parametrizacao-constantes-financeiras.md).

---

## Q1 · Como a estrutura de módulos vai evitar importações cruzadas de infraestrutura?

**Resposta curta:** três camadas internas por contexto + portas em `application` + adapters em `infrastructure`. Nenhum `import` de outro contexto para fora de `application.port`.

**Layout obrigatório (deriva direto do ADR-001 § "Estrutura lógica alvo"):**

```text
br/gov/sifap/<context>/
  domain/          ← entidades de domínio puras (sem JPA, sem Spring), VOs, regras
  application/
    port/          ← interfaces que outro contexto OU a infra precisa consumir
    service/       ← casos de uso, orquestração, @Transactional
    dto/           ← request/response da camada de aplicação
  infrastructure/
    persistence/   ← @Entity JPA, repositórios Spring Data, mapeadores
    rest/          ← @RestController, exception handlers locais
    integration/   ← gateways externos (CNAB, SIAFI), adapters de outros contextos
```

**Regra de ouro:** `payment.application.service.PaymentService` precisa de dado de beneficiário? Declara uma `BeneficiaryQueryPort` em `payment.application.port`. Quem **implementa** essa porta vive em `payment.infrastructure.integration.BeneficiaryServiceAdapter`, e esse adapter pode chamar a porta pública de `beneficiary` (ex.: `beneficiary.application.port.BeneficiaryQueryService`). Ou seja: domínio nunca importa outro contexto; aplicação só importa portas (interfaces); infraestrutura sabe que `beneficiary` existe.

**Enforcement em PR:**

1. Lint estático com [ArchUnit](https://www.archunit.org/) — adicionar `ModuleBoundariesTest` rodando no `verify`. Regras mínimas:
    - `payment.domain..` não pode depender de `org.springframework..` nem de `jakarta.persistence..`.
    - `payment.domain..` e `payment.application..` não podem depender de `..beneficiary..`, `..admin..` exceto `..port..` ou `..application.port..`.
    - `payment.infrastructure..` não pode ser referenciado por `payment.domain..` nem `payment.application..`.
2. Code review estrutural pelo Software Architect em qualquer PR que toque pacote raiz de contexto.

**Atalho para o estado atual:** o protótipo está em `com.sifap.<context>` plano. Antes do primeiro REQ-PAY ser estendido, o Par 3 abre PR único de **rename + split**:

- `com.sifap` → `br.gov.sifap`;
- mover `Payment.java` (entidade JPA) para `payment.infrastructure.persistence`;
- mover `PaymentController` para `payment.infrastructure.rest`;
- manter `PaymentService` em `payment.application.service`;
- criar pacote `payment.domain` com agregado puro (sem JPA);
- introduzir `BeneficiaryQueryPort` e `SocialProgramQueryPort` em `payment.application.port`.

Esse PR é estrutural, sem mudança de comportamento. Cobertura: testes existentes têm que passar inalterados.

---

## Q2 · Como será garantida a imutabilidade de auditoria no banco e na aplicação?

**Resposta curta:** três camadas de defesa — schema, ORM, processo. Falha em qualquer uma é defeito P0.

### Camada 1 — Banco (PostgreSQL)

- Tabela `audit_event` é append-only. Migration Flyway:

  ```sql
  REVOKE UPDATE, DELETE ON audit_event FROM application_role;
  CREATE OR REPLACE FUNCTION audit_event_block_update_delete()
  RETURNS trigger AS $$
  BEGIN
      RAISE EXCEPTION 'audit_event is append-only (REQ-AUD-001 / ADR-003)';
  END;
  $$ LANGUAGE plpgsql;

  CREATE TRIGGER audit_event_no_update BEFORE UPDATE ON audit_event
      FOR EACH ROW EXECUTE FUNCTION audit_event_block_update_delete();
  CREATE TRIGGER audit_event_no_delete BEFORE DELETE ON audit_event
      FOR EACH ROW EXECUTE FUNCTION audit_event_block_update_delete();
  ```

- Coluna `created_at` `NOT NULL DEFAULT now()`; sem coluna `updated_at`.
- Index pelo natural do contexto: `(entity_type, entity_id, created_at DESC)`.

### Camada 2 — JPA/Hibernate

- Entidade `AuditEvent` marcada com `@Immutable` (Hibernate) e sem `@Setter` em campos de domínio; construtor único + factory.
- Repositório `AuditEventRepository` **não estende `JpaRepository`**; estende `Repository<AuditEvent, Long>` e expõe apenas `save(...)` e queries de leitura. Nada de `delete*`, `update*` ou `saveAndFlush(...)` que possa ser usado para reescrita.
- `@Version` ausente — não há reconciliação otimista porque não há mutação.

### Camada 3 — Aplicação

- `AuditService.record(...)` chamado **fora** da transação principal do domínio (`Propagation.REQUIRES_NEW`) ou via listener pós-commit Spring (`@TransactionalEventListener(phase = AFTER_COMMIT)`). Decisão final do TL, mas auditoria **não pode ser rollbackada** junto com domínio.
- Mascaramento obrigatório de PII (CPF como `XXX.XXX.NNN-NN`) no payload **antes** de gravar. Helper compartilhado em `audit.application.MaskingPolicy`.
- ArchUnit: nenhum service fora de `audit.*` chama métodos de `AuditEventRepository` diretamente — só `AuditService`.

### Tradeoff explícito

Se o gravar de auditoria falhar e o domínio commitar, fica buraco na trilha. Mitigação: estratégia "outbox" — primeiro `INSERT INTO audit_event` em transação local mais externa, com retry assíncrono para casos de falha de write na infra. ADR de evolução abre depois do MVP se necessário.

---

## Q3 · Qual a estratégia para manter a ordenação por CPF no batch sem degradar a execução?

**Resposta curta:** ordenação no banco, não no Java. Index correto + paginação keyset.

### Diagnóstico

Hoje [PaymentService.generateCycle](../src/main/java/com/sifap/payment/PaymentService.java#L48) carrega **toda** a lista de ativos em memória e ordena com `Comparator.comparing(Beneficiary::getCpf)`. Para 2,3 milhões de beneficiários (volume real do legado), isso quebra heap.

### Recomendação

1. **Mover a ordenação para o banco.**
    - Index: `CREATE INDEX idx_beneficiary_active_cpf ON beneficiary (cpf) WHERE status = 'A';` (índice parcial — só ativos, mais barato).
    - Repositório expõe `Stream<Beneficiary>` ou `Page<Beneficiary>` com `ORDER BY cpf ASC`.
2. **Processar em chunks com keyset pagination**, não `LIMIT/OFFSET`:
    - `WHERE status='A' AND cpf > :lastSeenCpf ORDER BY cpf ASC LIMIT 1000`.
3. **Stream JPA + `@Transactional(readOnly = true)`** com `fetch_size` configurado:
    - `@QueryHints({@QueryHint(name = HINT_FETCH_SIZE, value = "1000")})`
    - `@Transactional(propagation = REQUIRES_NEW)` por chunk para não inflar log do PostgreSQL.
4. **Ordenação determinística para retomada**: o ID do `cycle_execution` registra `last_processed_cpf`; se o batch falhar, retomada começa de `cpf > last_processed_cpf`.
5. **Validação cruzada com Par 4 (DBA):** EXPLAIN ANALYZE da query do batch tem que mostrar `Index Scan using idx_beneficiary_active_cpf`. Se aparecer `Sort` no plano, falhou.

### Performance esperada

- Memória: ≈ 1 chunk em RAM (1 k beneficiários × ~500 B = 500 KB).
- Custo I/O: 2,3 M / 1 k = 2 300 chunks; com keyset, custo do `WHERE cpf > X` é O(log n) por chunk.
- Janela mensal de batch comporta com folga.

---

## Q4 · Onde o FATOR-K ficará configurado e como isso será coberto por testes?

**Resposta curta:** [ADR-004](ADR-004-parametrizacao-constantes-financeiras.md) decide. Bean `SifapFinancialProperties`. Testes unitários parametrizados + regressão contra vetor legado.

**Síntese das obrigações:**

1. Bean `br.gov.sifap.admin.application.SifapFinancialProperties` com `@ConfigurationProperties(prefix = "sifap.financial")`. Propriedades: `factorKConstant`, `nonJudicialCapRatio`, `christmasBonusRatio`.
2. `application.yml`:

    ```yaml
    sifap:
      financial:
        factor-k-constant: 0.347215
        non-judicial-cap-ratio: 0.30
        christmas-bonus-ratio: 0.15
    ```

3. Em produção, valores vêm de env vars (`SIFAP_FINANCIAL_FACTOR_K_CONSTANT`, etc.) carregadas via Key Vault / Managed Identity. Nada de secrets em `application-prod.yml`.
4. `SocialProgramService.create(...)` aplica a fórmula REQ-ADM-004 usando `properties.getFactorKConstant()` em `BigDecimal` com `RoundingMode.DOWN` (REQ-PAY-004 paridade legada).
5. `PaymentService` recebe o bean por construtor — nunca lê `Environment` ou `@Value` direto.

**Cobertura de testes (mínimo aceitável para fechar PR):**

- **T1 (unit):** `SocialProgramServiceTest#aplicaFatorK` parametrizado com vetor `(VLR_BASE, FATOR_REAJ, esperado)` espelhando os 3 critérios de aceitação do REQ-ADM-004.
- **T2 (regressão):** vetor extraído do dump legado, congelado em `src/test/resources/legacy/cadprog-fixtures.csv`. Falha qualquer divergência > R$ 0,01.
- **T3 (config sobrescrita):** `@SpringBootTest(properties = "sifap.financial.factor-k-constant=0.5")` — confirma que mudança de propriedade muda o resultado.
- **T4 (validação):** valor negativo ou > 1.0 em `nonJudicialCapRatio` faz a aplicação **falhar a inicialização** (não silenciar).
- **T5 (paridade descontos):** matriz de descontos cruzando `nonJudicialCapRatio` com cenários judicial puro / misto / só não-judicial — protege contra regressão em REQ-PAY-001/REQ-PAY-002.

---

## Próximos passos para o Par 3 (sequência sugerida)

1. **PR 1 (estrutural, sem comportamento):** rename `com.sifap` → `br.gov.sifap` + split em `domain/application/infrastructure` por contexto + introdução de `ArchUnit`.
2. **PR 2 (config):** `SifapFinancialProperties` + refactor de `PaymentService` para injetar o bean (resolve hardcodes de 0.30 e 0.15).
3. **PR 3 (REQ-ADM-004):** `SocialProgramService` aplica fórmula do FATOR-K, usando o bean.
4. **PR 4 (audit hardening):** migration de gatilhos + `AuditService` com `REQUIRES_NEW`.
5. **PR 5 (batch ordenação):** index parcial + keyset pagination em `generateCycle`.

A cada PR estrutural, **acionar o Par 2 (Software Architect) para code review** antes do merge.
