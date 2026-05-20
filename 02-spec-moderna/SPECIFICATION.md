# SPECIFICATION — SIFAP 2.0

**Versão:** 0.1.1 (Estágio 2 — sign-off PO concluído)
**Time:** Rosa-02
**Data:** 20/05/2026
**Responsável:** Par 2 — Enterprise Architect + Software Architect
**Aprovado pelo Product Owner:** ☑ Sim, em 20/05/2026 na passagem H2

**Origem dos requisitos:**
- `01-arqueologia/business-rules-catalog.md` (BR-001 a BR-015)
- `01-arqueologia/mysteries-found.md` (MYS-001 a MYS-010)
- `01-arqueologia/discovery-report.md` (priorização Par 1)

---

## Escopo

Este documento cobre os 4 bounded contexts do SIFAP 2.0:

| Contexto      | Prioridade (Par 1) | REQ-IDs neste doc |
|---------------|--------------------|-------------------|
| `beneficiary` | P1 (via elegibilidade) | REQ-BEN-001 a REQ-BEN-006 |
| `payment`     | P0 (núcleo de valor) | REQ-PAY-001 a REQ-PAY-005 |
| `admin`       | P1 (via programas)  | REQ-ADM-001 a REQ-ADM-004 |
| `audit`       | P1 (via compliance) | REQ-AUD-001 a REQ-AUD-002 |

**Fora de escopo v1.0:** integração com SIAFI, relatórios analíticos avançados,
biometria (campos `HASH-DIGITAL` não implementados no legado), integração Banco Real descontinuada.

---

## Contexto: `beneficiary`

### REQ-BEN-001 · CPF único por beneficiário

```yaml
REQ-BEN-001:
  pattern: unwanted
  text: "O SIFAP não deve permitir o cadastro de um beneficiário cujo CPF já exista
         no sistema, independentemente do status do registro existente."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/CADBENEF.NSN#L143-L147
  business_rule: BR-001
  acceptance:
    - "Dado CPF '12345678901' já cadastrado com status A,
       ao tentar incluir novo beneficiário com mesmo CPF → retorna erro 409 com mensagem 'CPF já cadastrado'."
    - "Dado CPF '12345678901' cadastrado com status C (cancelado),
       ao tentar incluir → ainda retorna erro 409 (regra é independente de status)."
    - "Dado CPF inexistente no sistema → inclusão prossegue normalmente."
  priority: P0
  risk: ALTO
```

### REQ-BEN-002 · Suspensão automática para beneficiários acima de 75 anos

```yaml
REQ-BEN-002:
  pattern: event-driven
  text: "Quando um beneficiário for cadastrado com idade superior a 75 anos,
         o SIFAP deve definir seu status inicial como SUSPENDED (S),
         independentemente de qualquer status informado pelo operador."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/CADBENEF.NSN#L163-L168
  business_rule: BR-002
  mystery_ref: MYS-001
  acceptance:
    - "Dado beneficiário com data de nascimento que resulte em idade 76,
       ao incluir → status gravado é S, não A."
    - "Dado beneficiário com idade exata de 75 anos → status inicial é A (limite é >75, não >=75)."
    - "Evento de auditoria deve registrar a suspensão automática com motivo 'AGE_RULE'."
  priority: P1
  risk: ALTO
  note: "Regra identificada como MYS-001 — mudança silenciosa de status. Tornar explícita
         via evento de auditoria para rastreabilidade."
```

### REQ-BEN-003 · Limite máximo de dependentes

```yaml
REQ-BEN-003:
  pattern: unwanted
  text: "O SIFAP não deve permitir o cadastro de mais de 5 dependentes por beneficiário."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/CADDEPEND.NSN#L63-L66
  business_rule: BR-004
  mystery_ref: MYS-002
  acceptance:
    - "Dado beneficiário com 5 dependentes cadastrados,
       ao tentar incluir o 6º → retorna erro 422 com mensagem 'Limite de dependentes atingido'."
    - "Dado beneficiário com 4 dependentes → inclusão do 5º é aceita."
  priority: P1
  risk: ALTO
  note: "O DDM BENEFICIARIO define PE com até 10 ocorrências (MYS-002); a regra de negócio
         impõe limite 5 via código. O sistema moderno deve respeitar o limite de negócio, não
         a capacidade física do DDM."
```

### REQ-BEN-004 · Bloqueio de dependente para beneficiário cancelado ou desligado

```yaml
REQ-BEN-004:
  pattern: unwanted
  text: "O SIFAP não deve permitir inclusão de dependentes para beneficiários
         com status CANCELLED (C) ou DISMISSED (D)."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/CADDEPEND.NSN#L56-L59
  business_rule: BR-003
  acceptance:
    - "Dado beneficiário com status C → tentativa de adicionar dependente retorna erro 422."
    - "Dado beneficiário com status D → mesmo comportamento de bloqueio."
    - "Dado beneficiário com status S (suspenso) → inclusão de dependente é permitida."
  priority: P1
  risk: ALTO
```

### REQ-BEN-005 · Validação de UF no cadastro

```yaml
REQ-BEN-005:
  pattern: event-driven
  text: "Quando o campo UF for informado no cadastro de um beneficiário,
         o SIFAP deve validar que o valor corresponde a uma das 27 unidades
         federativas brasileiras reconhecidas."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/VALBENEF.NSN#L145-L159
  business_rule: BR-007
  acceptance:
    - "Dado UF = 'SP' → validação passa."
    - "Dado UF = 'XX' → retorna erro 400 com mensagem 'UF inválida'."
    - "Dado UF em branco (campo opcional) → validação não é executada."
  priority: P1
  risk: MÉDIO
```

### REQ-BEN-006 · Exceção de CPF especial governamental

```yaml
REQ-BEN-006:
  pattern: state-driven
  text: "Enquanto o CPF do beneficiário iniciar com os prefixos especiais
         000, 001, 002, 010, 011, 099, 100 ou 999,
         o SIFAP deve marcar o documento como válido e ignorar falhas de validação documental."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/VALDOCS.NSN#L168-L179
  business_rule: BR-015
  mystery_ref: MYS-007
  acceptance:
    - "Dado CPF '00012345678' (prefixo 000) → validação documental é bypassed, status VALID."
    - "Dado CPF '99912345678' (prefixo 999) → mesmo comportamento."
    - "Dado CPF '12312312300' (prefixo comum) → validação documental normal é executada."
  priority: P1
  risk: ALTO
  note: "Exceção governamental/teste confirmada em BR-009 e BR-015. Deve ser implementada
         como lista de prefixos configurável (não hardcoded), para facilitar auditoria futura."
```

---

## Contexto: `payment`

### REQ-PAY-001 · Teto de 30% para descontos não judiciais

```yaml
REQ-PAY-001:
  pattern: unwanted
  text: "O SIFAP não deve permitir que a soma dos descontos de tipos
         não-judiciais exceda 30% do valor bruto do pagamento."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/CALCDSCT.NSN#L136-L145
  business_rule: BR-006
  mystery_ref: MYS-006
  acceptance:
    - "Dado pagamento bruto R$ 1.000 e desconto tipo TAX de R$ 400
       → desconto aplicado é truncado em R$ 300 (30%)."
    - "Dado pagamento bruto R$ 1.000 e desconto tipo JUDICIAL de R$ 800
       → desconto aplicado é R$ 800 integralmente (sem teto)."
    - "Dado soma de descontos não-judiciais exatamente em 30% → aceito sem truncamento."
  priority: P0
  risk: CRÍTICO
```

### REQ-PAY-002 · Desconto judicial sem teto

```yaml
REQ-PAY-002:
  pattern: event-driven
  text: "Quando um desconto do tipo JUDICIAL (J) for aplicado a um pagamento,
         o SIFAP deve adicionar o valor integralmente ao total de descontos,
         sem aplicar o teto de 30%."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/CALCDSCT.NSN#L181-L186
  business_rule: BR-006
  mystery_ref: MYS-006
  acceptance:
    - "Desconto judicial de 80% do valor bruto é aceito sem truncamento."
    - "Múltiplos descontos judiciais somam sem limite de teto."
    - "Combinação de desconto judicial + não-judicial: apenas o não-judicial é limitado a 30%."
  priority: P0
  risk: CRÍTICO
  note: "Regra legal diferenciada (MYS-006). Tratar como bug causaria retenção indevida
         e passivo jurídico. Crítico para conformidade legal."
```

### REQ-PAY-003 · Décimo terceiro e abono natalino em dezembro

```yaml
REQ-PAY-003:
  pattern: complex
  text: "Enquanto o ciclo de pagamento for do mês de dezembro,
         quando o programa social for do tipo assistencial (A),
         o SIFAP deve calcular e incluir o décimo terceiro benefício
         e um abono natalino equivalente a 15% do valor bruto."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/CALCBENF.NSN#L257-L274
  business_rule: BR-006
  mystery_ref: MYS-004
  acceptance:
    - "Ciclo dezembro + programa tipo A + bruto R$ 1.000
       → pagamento inclui 13º de R$ 1.000 + abono de R$ 150."
    - "Ciclo novembro + mesmo beneficiário → sem 13º nem abono."
    - "Ciclo dezembro + programa tipo P (previdenciário) → sem abono natalino de 15%."
  priority: P0
  risk: CRÍTICO
  note: "Regra identificada em MYS-004. Se não replicada, pagamentos de dezembro divergem
         do legado com impacto social e financeiro alto."
```

### REQ-PAY-004 · Política de truncamento em correção retroativa

```yaml
REQ-PAY-004:
  pattern: ubiquitous
  text: "O SIFAP deve aplicar truncamento (não arredondamento) para 2 casas decimais
         em todos os cálculos de correção retroativa de pagamentos."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/CALCCORR.NSN#L170-L173
  mystery_ref: MYS-005
  acceptance:
    - "Dado valor calculado R$ 100,456 → valor gravado é R$ 100,45 (truncado, não arredondado)."
    - "Dado valor calculado R$ 100,999 → valor gravado é R$ 100,99."
    - "A mesma política deve ser aplicada consistentemente em TODOS os cálculos de correção
       para evitar divergência acumulada na reconciliação."
  priority: P1
  risk: ALTO
  note: "MYS-005: truncamento sistemático foi comportamento intencional do legado.
         Preservar para paridade de reconciliação histórica."
```

### REQ-PAY-005 · Geração de pagamentos apenas para beneficiários ativos

```yaml
REQ-PAY-005:
  pattern: event-driven
  text: "Quando um ciclo de pagamento for gerado,
         o SIFAP deve criar registros de pagamento exclusivamente para
         beneficiários com status ACTIVE (A)."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN#L198-L201
  mystery_ref: MYS-009
  acceptance:
    - "Dado ciclo gerado com 10 beneficiários ativos e 3 suspensos
       → exatamente 10 registros de pagamento criados."
    - "Beneficiário com status S, C, I ou D não gera pagamento no ciclo."
    - "A ordenação dos pagamentos gerados deve ser por CPF (ordem crescente),
       para compatibilidade com sistemas de conciliação downstream."
  priority: P0
  risk: ALTO
  note: "MYS-009: ordenação por CPF é dependência operacional de sistemas downstream
         (conciliação bancária). Deve ser preservada."
```

---

## Contexto: `admin`

### REQ-ADM-001 · Unicidade de código de programa social

```yaml
REQ-ADM-001:
  pattern: unwanted
  text: "O SIFAP não deve permitir o cadastro de um programa social
         com código (COD-PROGRAMA) já existente no sistema."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/CADPROG.NSN#L82-L84
  business_rule: BR-005
  acceptance:
    - "Dado COD-PROGRAMA 'BPC1' já cadastrado → tentativa de novo cadastro retorna erro 409."
    - "Dado COD-PROGRAMA inexistente → cadastro prossegue normalmente."
  priority: P1
  risk: ALTO
```

### REQ-ADM-002 · Elegibilidade por faixa etária por tipo de programa

```yaml
REQ-ADM-002:
  pattern: complex
  text: "Enquanto o programa social estiver ativo,
         quando a validação de elegibilidade for executada para um beneficiário,
         o SIFAP deve verificar a faixa etária conforme o tipo do programa:
         previdenciário (P) exige idade mínima de 60 anos;
         trabalho (T) exige idade entre 16 e 65 anos."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/VALELEG.NSN#L183-L196
  business_rule: BR-012
  acceptance:
    - "Beneficiário de 59 anos solicitando programa tipo P → inelegível (motivo: IDADE_MIN_P)."
    - "Beneficiário de 60 anos para programa tipo P → elegível por faixa etária."
    - "Beneficiário de 15 anos para programa tipo T → inelegível (motivo: IDADE_MIN_T)."
    - "Beneficiário de 66 anos para programa tipo T → inelegível (motivo: IDADE_MAX_T)."
    - "Programa tipo A (assistencial) → sem restrição etária por este requisito."
  priority: P1
  risk: ALTO
```

### REQ-ADM-003 · Elegibilidade automática para região especial 99

```yaml
REQ-ADM-003:
  pattern: event-driven
  text: "Quando a validação de elegibilidade for executada para um beneficiário
         com código de região igual a 99,
         o SIFAP deve aprovar a elegibilidade automaticamente,
         sem executar as demais verificações de critério."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/VALELEG.NSN#L107-L111
  business_rule: BR-010
  mystery_ref: MYS-008
  acceptance:
    - "Beneficiário com COD-REGIAO = 99, independentemente de renda ou idade
       → elegibilidade aprovada com motivo 'REGIAO_ESPECIAL'."
    - "Beneficiário com COD-REGIAO = 01 → fluxo normal de validação é executado."
    - "Evento de auditoria deve registrar aprovação automática por região especial."
  priority: P1
  risk: ALTO
  note: "MYS-008: exceção crítica de domínio. Omitir bloqueia públicos especiais
         (ex.: diplomáticos, regiões diferenciadas). Deve ser implementada como
         regra configurável e auditada, não hardcoded."
```

### REQ-ADM-004 · Ajuste do valor base do programa pelo FATOR-K

```yaml
REQ-ADM-004:
  pattern: event-driven
  text: "Quando um novo programa social for cadastrado,
         o SIFAP deve calcular e persistir o valor base ajustado
         aplicando a fórmula: VLR-BASE-AJUSTADO = VLR-BASE-INPUT × (1.00 + FATOR-REAJ × 0.347215)."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/CADPROG.NSN#L86-L88
  business_rule: BR-006
  mystery_ref: MYS-003
  acceptance:
    - "Dado VLR-BASE-INPUT = 500.00 e FATOR-REAJ = 0.10
       → FATOR-K = 1.00 + (0.10 × 0.347215) = 1.0347215
       → VLR-BASE gravado = 500.00 × 1.0347215 = 517.36 (truncado 2 casas)."
    - "Dado FATOR-REAJ = 0.00 → VLR-BASE gravado = VLR-BASE-INPUT (sem ajuste)."
    - "O campo VLR-BASE retornado pela API GET /api/v1/admin/programs/{id}
       deve refletir o valor já ajustado, não o valor bruto informado."
    - "O valor 0.347215 deve ser externalizável via configuração de sistema
       (chave FATOR_K_CONSTANTE), para permitir auditoria e eventual correção
       sem necessidade de recompilação."
  priority: P1
  risk: CRÍTICO
  note: "MYS-003: constante 0.347215 sem origem documental, inserida em ago/2008 por solicitação
         da SENARC. O legado grava VLR-CALC (ajustado) no campo VLR-BASE do DDM PROGRAMA-SOCIAL
         (linha MOVE #VLR-CALC TO PROGRAMA-V.VLR-BASE, CADPROG.NSN#L92).
         Qualquer migração que use o VLR-BASE bruto como entrada de cálculo de benefício
         produzirá valores errados para todos os programas com FATOR-REAJ != 0."
```

---

## Contexto: `audit`

### REQ-AUD-001 · Trilha de auditoria imutável

```yaml
REQ-AUD-001:
  pattern: ubiquitous
  text: "O SIFAP deve registrar um evento de auditoria imutável para toda operação
         de inclusão, alteração ou exclusão em entidades de beneficiário, pagamento
         e programa social, contendo o estado anterior e o estado posterior da entidade."
  source_legacy: 01-arqueologia/legado-sifap/adabas-ddms/AUDITORIA.ddm
  acceptance:
    - "Toda operação de inclusão (IN) → evento de auditoria com state_before=null e state_after preenchido."
    - "Toda operação de alteração (AL) → evento com state_before e state_after preenchidos em JSON."
    - "Tentativa de UPDATE ou DELETE em audit_entry → retorna erro 403 (Forbidden)."
    - "CPF nos eventos de auditoria deve ser mascarado no formato XXX.XXX.NNN-NN para consulta via API."
  priority: P0
  risk: CRÍTICO
  note: "Obrigatoriedade legal: IN-TCU 63/2010. Retenção mínima: 10 anos (Lei 8.159/1991, art. 14).
         Ver ADR-003."
```

### REQ-AUD-002 · Visibilidade de exclusões no relatório de auditoria

```yaml
REQ-AUD-002:
  pattern: ubiquitous
  text: "O SIFAP deve exibir eventos de auditoria do tipo exclusão (EX)
         nos relatórios e consultas de auditoria, sem filtragem automática."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/RELAUDIT.NSN#L104-L111
  mystery_ref: MYS-010
  acceptance:
    - "Consulta ao endpoint GET /api/v1/audit retorna eventos de ação EX."
    - "Relatório de auditoria exportado em CSV inclui linhas com acao='EX'."
    - "Nenhum filtro automático remove eventos EX da visualização padrão."
  priority: P1
  risk: ALTO
  note: "MYS-010: legado ocultava eventos EX no relatório. Comportamento corrigido na
         versão moderna — auditoria deve ser completa e rastreável (IN-TCU 63/2010)."
```

---

## Rastreabilidade Consolidada

| REQ-ID | Bounded Context | BR/MYS de origem | Padrão EARS | Prioridade |
|--------|----------------|------------------|-------------|------------|
| REQ-BEN-001 | beneficiary | BR-001 | Unwanted | P0 |
| REQ-BEN-002 | beneficiary | BR-002, MYS-001 | Event-driven | P1 |
| REQ-BEN-003 | beneficiary | BR-004, MYS-002 | Unwanted | P1 |
| REQ-BEN-004 | beneficiary | BR-003 | Unwanted | P1 |
| REQ-BEN-005 | beneficiary | BR-007 | Event-driven | P1 |
| REQ-BEN-006 | beneficiary | BR-015, MYS-007 | State-driven | P1 |
| REQ-PAY-001 | payment | BR-006, MYS-006 | Unwanted | P0 |
| REQ-PAY-002 | payment | BR-006, MYS-006 | Event-driven | P0 |
| REQ-PAY-003 | payment | MYS-004 | Complex | P0 |
| REQ-PAY-004 | payment | MYS-005 | Ubiquitous | P1 |
| REQ-PAY-005 | payment | MYS-009 | Event-driven | P0 |
| REQ-ADM-001 | admin | BR-005 | Unwanted | P1 |
| REQ-ADM-002 | admin | BR-012, BR-013 | Complex | P1 |
| REQ-ADM-003 | admin | BR-010, MYS-008 | Event-driven | P1 |
| REQ-ADM-004 | admin | BR-006, MYS-003 | Event-driven | P1 |
| REQ-AUD-001 | audit | AUDITORIA.ddm | Ubiquitous | P0 |
| REQ-AUD-002 | audit | MYS-010 | Ubiquitous | P1 |

**Total: 17 REQ-IDs** (mínimo exigido: 12 ✅)
**REQ-IDs P0 (críticos):** REQ-BEN-001, REQ-PAY-001, REQ-PAY-002, REQ-PAY-003, REQ-PAY-005, REQ-AUD-001

---

## Itens pendentes de validação (após sign-off H2)

- [x] Par 1 (PO) confirmou prioridade P0/P1 dos REQ-IDs na passagem H2 em 20/05/2026
- [x] Investigar `FATOR-K` (campo BG do DDM PROGRAMA-SOCIAL, inserido 2008 sem doc): evidência encontrada em `CADPROG.NSN#L87-L88` (cálculo implícito) e `PROGRAMA-SOCIAL.ddm#L39` (campo BG); PO condiciona implementação à formalização de REQ-ADM-004
- [ ] Confirmar com Par 4 (DBA) política de particionamento do schema `audit` para suportar retenção de 10 anos
- [ ] Alinhar com Par 3 (TL/Dev) se a ordenação por CPF no batch (REQ-PAY-005) tem impacto de performance no PostgreSQL

**Status de passagem:** H2 liberada para início do Estágio 3, condicionada ao alinhamento técnico contínuo entre Par 2, Par 3 e Par 4 sobre particionamento de auditoria e performance do batch.
