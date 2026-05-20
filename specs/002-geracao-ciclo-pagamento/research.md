<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Research — 002 Geração de Ciclo de Pagamento

## Decisões Investigadas

### B4. Pacote de decisões de contrato e modelagem (Par 2 · Software Architect)

| Tema | Decisão B4 | Dono | Dependências e impacto |
|------|------------|------|------------------------|
| IDs (`UUID` x `Long`) | Padronizar `UUID` em contratos e entidades do ciclo; `Long` não é exposto em API. | Par 2 · Software Architect | EA referenda consistência arquitetural; Par 3 ajusta refactor de DTO/entidade; Par 4 valida tipo e índices em migration. |
| Competência (`AAAAMM` x `YYYY-MM`) | API usa `YYYY-MM` (ISO), com conversão de fronteira para modelo interno baseado em `YearMonth`/`DATE`. | Par 2 · Software Architect | Par 3 implementa serializer/deserializer; Par 4 garante coluna e conversão em migration sem ambiguidade. |
| Path do endpoint | Manter padrão REST versionado em `/api/v1/payment-cycles`. | Par 2 · Software Architect | EA referenda padrão de naming; Par 3 ajusta controller/roteamento; contrato OpenAPI permanece estável. |
| Split de descontos | Modelagem 1:N em `payment_discount`, preservando ordem de processamento para aderência legada e compliance. | Par 2 · Software Architect | Par 2 (Architect) deve confirmar índices `payment_discount(payment_id, processing_order)` para escala de 3.8M registros/mês; Par 4 impactado diretamente na migration; Par 3 impactado no refactor de cálculo/agregação; PO ciente por exigência de compliance. |

**Status:** consolidado para implementação no Estágio 3.

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
