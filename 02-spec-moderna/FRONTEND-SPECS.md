# Frontend Specs — SIFAP 2.0

**Versão:** 0.1.0  
**Data:** 20/05/2026  
**Stack:** Next.js 15 + TypeScript strict + Tailwind CSS + shadcn/ui  
**Responsável:** Par 3 — Developer

---

## Objetivo

Mapear os 17 REQ-IDs da SPECIFICATION.md para telas, componentes e fluxos de usuário no frontend.
Cada tela deve respeitar as fronteiras de bounded context e apenas expor dados relevantes ao contexto.

## Convenções

- **Server Components (padrão):** buscam dados, sem JavaScript no cliente.
- **Client Components:** somente para interatividade (forms, modais, botões).
- **Mascaramento sensível:** CPF sempre em formato `XXX.XXX.NNN-NN` na UI.
- **Imutabilidade visual:** auditoria nunca exibe botão de edição ou delete.

---

## Contexto: `beneficiary`

### Tela: Cadastro e Consulta de Beneficiários

**REQ-IDs atendidos:** REQ-BEN-001, REQ-BEN-002, REQ-BEN-003, REQ-BEN-004, REQ-BEN-005, REQ-BEN-006

#### Componentes principais

- `BeneficiaryForm` (Client Component)
  - Campo CPF com validação de unicidade (REQ-BEN-001, REQ-BEN-006)
  - Campo data de nascimento com cálculo de idade (REQ-BEN-002)
  - Seletor de UF com validação (REQ-BEN-005)
  - Seletor de dependentes com limite de 5 (REQ-BEN-003)

- `BeneficiaryList` (Server Component)
  - Exibe beneficiários com status
  - Filtra por status (ativo, suspenso, cancelado, desligado)
  - Desabilita ações de inclusão de dependente para status C/D (REQ-BEN-004)

- `BeneficiaryDetails` (Server Component)
  - Exibe data de cadastro, alteração, status
  - Lista dependentes com status individual
  - CPF mascarado na exibição

#### Fluxo de usuário (operador MDAS)

1. Clica em "Novo Beneficiário"
2. Preenche CPF (sistema valida unicidade) → REQ-BEN-001
3. Preenche data de nascimento (sistema calcula idade e ajusta status se > 75) → REQ-BEN-002
4. Seleciona UF (validação de 27 estados) → REQ-BEN-005
5. Adiciona até 5 dependentes (sistema rejeita 6º) → REQ-BEN-003
6. Tenta adicionar dependente a beneficiário cancelado → erro, campo bloqueado → REQ-BEN-004
7. Sistema cria registro e emite evento de auditoria → conecta com REQ-AUD-001

---

## Contexto: `payment`

### Tela: Geração e Consulta de Ciclo de Pagamento

**REQ-IDs atendidos:** REQ-PAY-001, REQ-PAY-002, REQ-PAY-003, REQ-PAY-004, REQ-PAY-005, REQ-AUD-001

#### Componentes principais

- `CycleGenerationForm` (Client Component)
  - Seletor de mês/ano do ciclo
  - Botão "Gerar Ciclo"
  - Exibe status de progresso e erros (assíncrono)

- `PaymentList` (Server Component)
  - Exibe pagamentos gerados no ciclo
  - Filtros: status, beneficiário (por CPF), faixa de valor
  - Ordenação por CPF crescente (REQ-PAY-005)
  - Exibe apenas beneficiários ativos (REQ-PAY-005)

- `PaymentDetails` (Server Component)
  - Exibe valor bruto, descontos (judicial vs não-judicial), valor líquido
  - Mostra cálculo: teto 30% não-judicial, sem teto judicial (REQ-PAY-001, REQ-PAY-002)
  - Exibe 13º e abono de 15% se ciclo é dezembro + programa assistencial (REQ-PAY-003)
  - Exibe política de truncamento em correção (2 casas decimais) (REQ-PAY-004)

- `DeductionBreakdown` (Server Component)
  - Tabela: tipo de desconto, valor bruto, tipo (J/NJ), valor aplicado, justificativa
  - Flag visual se desconto foi truncado (REQ-PAY-001)

- `AuditTrail` (Server Component)
  - Exibe evento de criação do pagamento com usuário, data/hora UTC
  - Não permite edição nem exclusão visual (botões desabilitados)
  - Mascaramento de CPF em todos os eventos (REQ-AUD-001)

#### Fluxo de usuário (operação financeira)

1. Clica em "Gerar Ciclo de Pagamento"
2. Seleciona mês/ano → Confirma
3. Sistema processa (backend ordena por CPF) → REQ-PAY-005
4. Lista exibe pagamentos gerados
5. Clica em pagamento específico → vê breakdow de cálculo (descontos, 13º, abono)
6. Clica em "Auditoria" → vê trilha imutável com timestamp UTC
7. Tenta editar ou deletar auditoria → não consegue (UI não oferece ação)

---

## Contexto: `admin`

### Tela: Cadastro e Consulta de Programas Sociais

**REQ-IDs atendidos:** REQ-ADM-001, REQ-ADM-002, REQ-ADM-003, REQ-ADM-004

#### Componentes principais

- `ProgramForm` (Client Component)
  - Campo código do programa (validação de unicidade) → REQ-ADM-001
  - Campo nome do programa
  - Seletor de tipo (P=previdenciário, T=trabalho, A=assistencial) → REQ-ADM-002
  - Campo valor base (entrada bruta)
  - Campo FATOR-REAJ (reajuste anual)
  - **Exibição:** "Valor Base Ajustado = {valor_bruto} × (1.00 + {fator_reaj} × 0.347215)" → REQ-ADM-004
  - Checkbox "Eleição automática para região 99" → REQ-ADM-003

- `ProgramList` (Server Component)
  - Exibe programas com código, tipo, valor base ajustado
  - Filtro por tipo
  - Exibe badge "Eleição automática" se aplicável → REQ-ADM-003

- `EligibilityRuleDisplay` (Server Component)
  - Tipo P: "Exige idade ≥ 60 anos"
  - Tipo T: "Exige idade entre 16 e 65 anos"
  - Tipo A: "Sem restrição etária"
  - Região 99: "Eleição automática, sem validações adicionais" → REQ-ADM-003

#### Fluxo de usuário (administrador)

1. Clica em "Novo Programa Social"
2. Preenche código único (sistema valida unicidade) → REQ-ADM-001
3. Seleciona tipo (P/T/A)
4. Preenche valor base (ex: 500.00)
5. Preenche FATOR-REAJ (ex: 0.10)
6. Sistema mostra "Valor Base Ajustado = 517.36" (cálculo do FATOR-K) → REQ-ADM-004
7. Marca "Eleição automática para região 99" (opcional)
8. Salva → backend persiste valor ajustado como VLR-BASE
9. Tela de consulta exibe regras de elegibilidade por tipo → REQ-ADM-002

---

## Contexto: `audit`

### Tela: Relatório e Consulta de Auditoria

**REQ-IDs atendidos:** REQ-AUD-001, REQ-AUD-002

#### Componentes principais

- `AuditSearch` (Client Component)
  - Filtros: período (data início/fim), tipo de ação (IN/AL/EX), entidade (beneficiary/payment/program)
  - Campo de busca por identificador de registro (CPF, código de programa, ID de pagamento)
  - Botão "Exportar como CSV"

- `AuditLog` (Server Component)
  - Tabela: ação, entidade, identificador, usuário, data/hora UTC, estado anterior, estado posterior
  - **Inclui eventos EX (exclusão)** sem filtro automático → REQ-AUD-002
  - CPF sempre mascarado (XXX.XXX.NNN-NN) → REQ-AUD-001
  - Estados em formato JSON para alterações
  - Paginação ou scroll infinito

- `AuditDetails` (Server Component)
  - Modal/drawer exibindo um evento completo
  - Estado anterior (JSON formatado)
  - Estado posterior (JSON formatado)
  - Diff visual (campos que mudaram em destaque)
  - Nunca oferece botão de edição ou exclusão

#### Fluxo de usuário (auditor)

1. Clica em "Auditoria"
2. Define período (ex: últimos 30 dias)
3. Seleciona tipo de ação: "Todos" ou "Exclusão" (REQ-AUD-002)
4. Seleciona entidade (ex: beneficiary)
5. Sistema lista eventos (incluindo EX se selecionado) → REQ-AUD-002
6. Clica em evento → exibe estado anterior e posterior
7. CPF aparece como "XXX.XXX.NNN-NN" (REQ-AUD-001)
8. Clica em "Exportar CSV" → download sem filtro de ação (EX é incluído)

---

## Mapeamento REQ-ID → Telas

| REQ-ID | Tela Primária | Componente Principal | Ação Usuário |
|--------|---------------|----------------------|--------------|
| REQ-BEN-001 | Cadastro de Beneficiários | BeneficiaryForm | Preencher CPF → validação de unicidade |
| REQ-BEN-002 | Cadastro de Beneficiários | BeneficiaryForm | Preencher data de nascimento → cálculo de idade + status automático |
| REQ-BEN-003 | Cadastro de Beneficiários | BeneficiaryForm | Adicionar dependentes → limite 5 |
| REQ-BEN-004 | Cadastro de Beneficiários | BeneficiaryForm | Tentar adicionar dependente a cancelado/desligado → bloqueio |
| REQ-BEN-005 | Cadastro de Beneficiários | BeneficiaryForm | Preencher UF → validação de 27 estados |
| REQ-BEN-006 | Cadastro de Beneficiários | BeneficiaryForm | CPF com prefixo especial (000, 001, 002, 010, 011, 099, 100, 999) → bypass documental |
| REQ-PAY-001 | Consulta de Pagamentos | DeductionBreakdown | Visualizar desconto → vê teto de 30% aplicado |
| REQ-PAY-002 | Consulta de Pagamentos | DeductionBreakdown | Visualizar desconto judicial → vê sem teto |
| REQ-PAY-003 | Consulta de Pagamentos | PaymentDetails | Ciclo dezembro + programa assistencial → exibe 13º + abono 15% |
| REQ-PAY-004 | Consulta de Pagamentos | PaymentDetails | Visualizar valor de correção → vê truncamento 2 casas |
| REQ-PAY-005 | Geração e Consulta de Ciclo | CycleGenerationForm, PaymentList | Gerar ciclo → ordenação por CPF, apenas ativos |
| REQ-ADM-001 | Cadastro de Programas | ProgramForm | Preencher código → validação de unicidade |
| REQ-ADM-002 | Cadastro de Programas | EligibilityRuleDisplay | Ver regras por tipo (P/T/A) → faixa etária |
| REQ-ADM-003 | Cadastro de Programas | ProgramForm, EligibilityRuleDisplay | Marcar "Eleição automática região 99" → eleição sem validação |
| REQ-ADM-004 | Cadastro de Programas | ProgramForm | Preencher valor base + FATOR-REAJ → exibe cálculo FATOR-K |
| REQ-AUD-001 | Auditoria | AuditLog, AuditDetails | Consultar trilha → imutável, CPF mascarado |
| REQ-AUD-002 | Auditoria | AuditLog | Filtrar por ação "Exclusão" → inclui eventos EX |

---

## Padrões de UI esperados

### 1. Validação e feedback

- Erros de validação (unicidade, limites, formatos) aparecem inline no campo.
- Toast notificações para operações assíncronas (ciclo em processamento, sucesso de salvamento).
- Indicador visual de carregamento (skeleton ou spinner).

### 2. Mascaramento de dados sensíveis

- CPF: `XXX.XXX.NNN-NN` em todas as exibições de usuário.
- Valores monetários: sempre com 2 casas decimais (sem arredondamento exibido, respeitando truncamento do backend).

### 3. Imutabilidade da auditoria

- Páginas de auditoria **nunca** oferecem botão de editar ou deletar.
- Estados anteriores exibidos em UI de somente leitura.
- Exportação de auditoria mantém todos os eventos (sem filtro de exclusão).

### 4. Responsividade

- Componentes mobile-first com Tailwind CSS.
- Tabelas em mobile: scroll horizontal ou card layout.

---

## Próximas iterações (fora do Estágio 3)

- Relatórios analíticos avançados (ex: beneficiários por região, tempo médio de pagamento).
- Dashboard executivo com métricas (beneficiários totais, ciclo atual, arrecadação).
- Integração com relatórios de SIAFI e conciliação bancária.
- Autenticação/autorização granular por perfil (operador, auditor, admin).

