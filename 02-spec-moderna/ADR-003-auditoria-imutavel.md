# ADR-003 — Auditoria Imutável e Conformidade Legal

**Data:** 20/05/2026
**Status:** Aceita
**Decisores:** Par 2 (EA + SA) · revisão Par 4 (DBA + QA)
**Origem no legado:** DDM AUDITORIA — comentário "REGISTRO IMUTAVEL - NAO PERMITE UPDATE/DELETE"
                      · "OBRIGATORIEDADE LEGAL: IN-TCU 63/2010" · "RETENCAO MINIMA: 10 ANOS"

---

## Contexto

O DDM AUDITORIA do legado tem três restrições explícitas documentadas desde 1997 e reforçadas
em 2010:

1. **Imutabilidade:** o arquivo não permite UPDATE nem DELETE — evidenciado pelo comentário
   no DDM e pela ausência de programas Natural que façam UPDATE neste arquivo.
2. **Obrigatoriedade legal:** IN-TCU 63/2010 exige trilha de auditoria para sistemas de
   pagamento de benefícios sociais.
3. **Retenção mínima:** 10 anos por força do Art. 14 da Lei 8.159/1991 (Lei de Arquivos).

Qualquer implementação moderna que viole essas restrições cria passivo legal para o órgão.

## Opções Consideradas

### Opção 1: Tabela de auditoria com soft delete (UPDATE allowed)
- **Descrição:** Implementar como tabela comum PostgreSQL, permitindo correções via UPDATE.
- **Vantagens:** Flexibilidade para corrigir registros errados.
- **Desvantagens:** Viola diretamente IN-TCU 63/2010 e o modelo legado. Risco legal imediato.
  **Rejeitada.**

### Opção 2: Append-only com constraints de banco (escolhida)
- **Descrição:** Schema `audit` no PostgreSQL com constraints de banco que impedem UPDATE/DELETE.
  Toda escrita é INSERT. Aplicação não recebe permissão de UPDATE/DELETE no schema `audit`.
- **Vantagens:** Conformidade legal garantida em duas camadas (aplicação + banco). Alinhado
  com o legado. Simples de implementar.
- **Desvantagens:** Registros errados não podem ser corrigidos — devem ser compensados com
  novo registro de correção (padrão de compensação).

### Opção 3: Event sourcing completo
- **Descrição:** Usar event sourcing como padrão arquitetural principal para auditoria.
- **Vantagens:** Rastreabilidade máxima.
- **Desvantagens:** Aumenta complexidade em toda a aplicação. Desnecessário dado o escopo.
  **Rejeitada.**

## Decisão

**Adotaremos auditoria append-only com constraints de banco** no schema `audit`:

```sql
-- Nenhuma permissão de UPDATE/DELETE para o usuário da aplicação no schema audit
REVOKE UPDATE, DELETE ON ALL TABLES IN SCHEMA audit FROM sifap_app;

-- Toda entrada de auditoria é INSERT
CREATE TABLE audit.audit_entry (
    id           BIGSERIAL PRIMARY KEY,
    occurred_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    action       VARCHAR(2)  NOT NULL,  -- IN/AL/EX/CO/LG/LO/BT/ER/AU/RE
    module       VARCHAR(8)  NOT NULL,
    entity_type  VARCHAR(4)  NOT NULL,  -- BENF/PGTO/PROG/ADMN
    entity_id    VARCHAR(15) NOT NULL,
    cpf_affected VARCHAR(11),
    state_before JSONB,
    state_after  JSONB,
    user_id      VARCHAR(8)  NOT NULL,
    ip_origin    INET
    -- SEM updated_at, SEM deleted_at, SEM soft_delete
);
```

**Regras de aplicação:**
1. O módulo `audit` expõe apenas um método: `record(AuditEvent event)` — sem update, sem delete.
2. Nenhum outro módulo lê diretamente do schema `audit` — apenas via API exposta pelo módulo.
3. A API de consulta de auditoria é somente leitura e exige perfil `AUDITOR`.
4. Dados de auditoria são mascarados em CPF para exposição via API (formato `XXX.XXX.NNN-NN`).

## Justificativa

A imutabilidade não é escolha arquitetural — é obrigação legal documentada desde o legado.
A implementação append-only com constraints de banco garante conformidade em duas camadas
independentes (código e banco), tornando impossível a violação acidental. O padrão de
compensação (novo registro corrige o anterior) é o mecanismo correto para tratar erros sem
violar imutabilidade.

## Consequências

- **Positivas:** Conformidade com IN-TCU 63/2010, rastreabilidade completa, modelo simples.
- **Negativas:** Correções de registros errados exigem registro de compensação — equipe precisa
  entender o padrão.
- **Impacto no DBA (Par 4):** Schema `audit` precisa de particionamento por data para suportar
  retenção de 10 anos com performance (ver DDM: "CONSULTAS PESADAS DEVEM USAR SUPERDESCRIPTOR S2").
- **Quando revisitar:** Se requisito de LGPD exigir anonimização de dados pessoais em auditoria
  (conflito com imutabilidade — exige análise jurídica antes de qualquer mudança).
