# ADR-003 — Trilha de auditoria imutável e retenção prolongada

- **Status:** Aceita
- **Data:** 20/05/2026
- **Decisores:** Par 2 — Enterprise Architect + Software Architect

## Contexto

O legado já trata auditoria como parte essencial do domínio operacional, e a spec moderna eleva essa responsabilidade a requisito explícito em REQ-AUD-001 e REQ-AUD-002.

Além da rastreabilidade técnica, o sistema precisa preservar histórico de inclusão, alteração e exclusão sem perda de evidência. A auditoria também não pode continuar com o comportamento legado de ocultar eventos de exclusão em relatórios.

## Decisão

Adotar uma trilha de auditoria append-only, com persistência própria, consulta explícita e proteção contra mutações destrutivas.

Diretrizes obrigatórias:

1. Toda inclusão, alteração e exclusão relevante gera evento de auditoria.
2. O registro de auditoria contém, no mínimo, ação, entidade, identificador de negócio, usuário, timestamp, estado anterior e estado posterior.
3. O armazenamento de auditoria não permite update nem delete pela aplicação.
4. Eventos de exclusão permanecem visíveis em consultas e exportações.
5. Campos sensíveis, como CPF, devem ser mascarados nas respostas de consulta.

## Consequências

### Positivas

- Atende à necessidade de rastreabilidade do domínio.
- Dá suporte a investigação, fiscalização e reconciliação histórica.
- Reduz o risco de perda silenciosa de evidência.

### Negativas

- Crescimento contínuo do volume de dados de auditoria.
- Necessidade de estratégia operacional de retenção e particionamento.
- Impacto potencial em consultas pesadas se o schema não for preparado adequadamente.

## Regras de implementação

1. O contexto `audit` possui schema e tabelas próprias.
2. Escrita de auditoria ocorre por contrato explícito, nunca por atualização manual ad hoc em múltiplos módulos.
3. O modelo de consulta deve contemplar filtros por período, usuário, entidade e ação.
4. O desenho físico de particionamento por data deve ser alinhado com o Par 4 antes da implementação final, mas a API e o modelo lógico já devem assumir retenção de longo prazo.

## Alternativas consideradas

### Auditoria como log técnico somente em arquivo

Rejeitada porque não fornece consulta estruturada, rastreabilidade de negócio nem evidência confiável para uso regulatório.

### Permitir exclusão lógica ou reescrita de eventos

Rejeitada porque enfraquece a confiança do histórico e conflita com o objetivo de imutabilidade.

## Relação com a spec

- Implementa a decisão de suporte para REQ-AUD-001 e REQ-AUD-002.
- Impõe guarda de fronteira para todos os demais módulos.
- Complementa [C4-DIAGRAMS.md](C4-DIAGRAMS.md) ao posicionar `audit` como contexto transversal, porém isolado.