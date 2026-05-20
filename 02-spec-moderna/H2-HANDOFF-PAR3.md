# H2 Handoff — Par 3 (Technical Lead + Developer)

**Origem:** Par 2 — Enterprise Architect + Software Architect  
**Destino:** Par 3 — Technical Lead + Developer  
**Data:** 20/05/2026  
**Status:** Liberado após sign-off do PO

## Objetivo

Entregar ao Par 3 o pacote arquitetural e de requisitos já aprovado para início do Estágio 3, reduzindo risco de interpretação errada, quebra de fronteira de módulos e regressão funcional em regras críticas do legado.

## Artefatos de referência

- [SPECIFICATION.md](SPECIFICATION.md)
- [C4-DIAGRAMS.md](C4-DIAGRAMS.md)
- [ADR-001-monolito-modular.md](ADR-001-monolito-modular.md)
- [ADR-002-estrategia-integracao.md](ADR-002-estrategia-integracao.md)
- [ADR-003-auditoria-imutavel.md](ADR-003-auditoria-imutavel.md)
- [scope-decisions.md](scope-decisions.md)

## Texto de repasse

Par 3, a passagem H2 está liberada com sign-off do PO concluído. A spec consolidada contém 17 REQ-IDs e define quatro bounded contexts obrigatórios: `beneficiary`, `payment`, `admin` e `audit`.

O foco inicial de implementação deve estar nos REQ-IDs P0, porque eles concentram o núcleo operacional do sistema e o maior risco de divergência com o legado: `REQ-BEN-001`, `REQ-PAY-001`, `REQ-PAY-002`, `REQ-PAY-003`, `REQ-PAY-005` e `REQ-AUD-001`.

Arquiteturalmente, a decisão é monolito modular. Isso significa que vocês devem implementar por contexto de negócio, evitando pacote genérico por camada e evitando qualquer acesso direto à infraestrutura de outro módulo. Comunicação entre módulos deve acontecer por contratos explícitos, services de aplicação ou eventos internos. O módulo `audit` é transversal, mas não pode virar atalho para acoplamento indevido.

Os dois riscos mais sensíveis nesta passagem são estes. Primeiro: `REQ-ADM-004` não é detalhe cosmético. O legado grava o valor base já ajustado pelo FATOR-K, então qualquer implementação que trate `VLR-BASE` como valor bruto vai produzir cálculo errado. Segundo: `REQ-PAY-005` exige preservação da ordenação por CPF no batch; isso precisa ser avaliado junto com estratégia de índice e performance no PostgreSQL.

Para a primeira iteração, a recomendação é atacar nesta ordem: núcleo de `payment`, trilha de `audit`, unicidade/estado mínimo de `beneficiary` e depois regras de `admin`. Se houver dúvida de fronteira, priorizem aderência aos ADRs e escalem rapidamente ao Par 2 antes de ampliar o código.

## Checklist de implementação inicial

- Criar estrutura de módulos coerente com os 4 bounded contexts.
- Implementar primeiro os REQ-IDs P0 antes de expandir P1.
- Garantir que `audit` seja append-only desde a primeira versão.
- Preservar a semântica do cálculo de desconto judicial e teto não judicial.
- Preservar a semântica do cálculo de dezembro e da geração de pagamentos apenas para beneficiários ativos.
- Não tratar `VLR-BASE` como valor bruto após cadastro de programa.
- Validar com Par 4 a estratégia física do schema `audit`.
- Validar com Par 2 qualquer dúvida sobre fronteiras, contratos ou evolução das integrações.

## Perguntas que o Par 3 deve responder cedo

1. Como a estrutura de módulos vai evitar importações cruzadas de infraestrutura?
2. Como será garantida a imutabilidade de auditoria no banco e na aplicação?
3. Qual a estratégia para manter a ordenação por CPF no batch sem degradar a execução?
4. Onde o FATOR-K ficará configurado e como isso será coberto por testes?