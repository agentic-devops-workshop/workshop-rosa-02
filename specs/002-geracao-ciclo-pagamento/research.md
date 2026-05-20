<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Research — 002 Geração de Ciclo de Pagamento

## Decisões Investigadas

### 1. Como tratar descontos judiciais
- Opções:
  - A) Aplicar teto único de 30% para todos os descontos.
  - B) Aplicar teto de 30% apenas para não judiciais e exceção para judiciais.
- Decisão: **B**.
- Motivo: aderência ao legado (`CALCDSCT.NSN`) e risco jurídico se houver truncamento indevido.

### 2. Como modelar elegibilidade especial da região 99
- Opções:
  - A) Tratamento explícito no serviço de elegibilidade.
  - B) Regra parametrizada em tabela de configuração.
- Decisão inicial: **A** para MVP, com evolução futura para B.
- Motivo: manter fidelidade imediata e reduzir tempo de implementação.

### 3. Como representar limite de dependentes
- Opções:
  - A) Limite operacional fixo em regra de serviço (5).
  - B) Limite por configuração dinâmica.
- Decisão inicial: **A**.
- Motivo: compatibilidade com legado operacional (`CADDEPEND.NSN`).

## Ambiguidades para Clarify
- Prefixos especiais de CPF devem ser tratados por feature flag no moderno?
- Regra de status `S` para idade > 75 deve ocorrer em cadastro, batch ou ambos?
- Exposição de motivo de bloqueio de elegibilidade deve ir ao payload de API?

## Fora de Escopo nesta feature
- Migração de integração bancária legada comentada.
- Biometria (`HASH-DIGITAL`) e integrações de autenticação além do token JWT já definido.
