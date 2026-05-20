<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Decisões de Escopo — SIFAP 2.0

![ESTÁGIO 02 Spec](https://img.shields.io/badge/ESTÁGIO-02%20Spec-00A4EF?style=for-the-badge) ![TIPO Worksheet](https://img.shields.io/badge/TIPO-Worksheet-1A1A1A?style=for-the-badge) ![PREENCHA Durante S2](https://img.shields.io/badge/PREENCHA-Durante%20S2-737373?style=for-the-badge)

> 🗺 **Você está aqui:** [Kit PT-BR](../README.md) → [Estágio 2](README.md) → **Scope Decisions**

> **Para quem é isto?** Este é um **artefato preenchido pelo time** durante o Estágio 2 (Spec Moderna).
>
> **O que você terá ao final do estágio:**
>
> 1. Este documento preenchido para sua feature
> 2. Rastreabilidade `source_legacy:` para cada REQ-ID
> 3. Sign-off do Product Owner antes da passagem H2
>
> 📘 **Guia passo a passo:** [`GUIDE.md`](GUIDE.md).


> Para cada funcionalidade encontrada no Estágio 1, decida: **Migrar**, **Descartar** ou **Evoluir**.
>
> - **Migrar**: trazer para o SIFAP 2.0 como está (mesma lógica, nova tecnologia)
> - **Descartar**: não trazer — funcionalidade obsoleta ou desnecessária
> - **Evoluir**: trazer E melhorar (nova UX, novo fluxo, nova capacidade)

**Time**: workshop-rosa-02
**Data**: 20/05/2026
**Edição**: Estágio 2 — Passagem H2
**Par 1 (Product Owner) responsável**: Par 1 (PO)

## Por que isso importa

O escopo é o que protege o time de chegar às 17h00 com 12 features pela metade. Se o Par 1 não cortar, o Estágio 3 não fecha. **Decisão difícil é tomada aqui, não no Estágio 3.**

## Como decidir

Pergunte de cada funcionalidade:

1. **Afeta o ciclo mensal de pagamento?** Sim → Migrar. Não → considere descartar.
2. **Tem uso documentado nos últimos 12 meses?** Não → descartar.
3. **Faz parte de um relatório regulatório obrigatório (TCU, CGU, BB)?** Sim → Migrar como está.
4. **Tem uma versão moderna mais barata de implementar?** Sim → Evoluir.

---

## Decisões por Funcionalidade

| #   | Funcionalidade            | Decisão                      | Justificativa | Regra de Negócio (BR-XXX) | Prioridade           |
| --- | ------------------------- | ---------------------------- | ------------- | ------------------------- | -------------------- |
| 1   | Cadastro de Beneficiários | Migrar | Núcleo de domínio; impacta elegibilidade e pagamento. | BR-001, BR-002, BR-008 | Alta |
| 2   | Consulta de Beneficiários | Evoluir | Manter regra de negócio e melhorar filtros/mascara de dados. | BR-008 | Média |
| 3   | Registro de Pagamentos    | Migrar | Fluxo crítico mensal e base de conciliação. | BR-006 | Alta |
| 4   | Processamento Batch       | Migrar | Processo essencial do ciclo operacional. | BR-006, BR-010 | Alta |
| 5   | Cálculo de Benefícios     | Evoluir | Preservar legado e explicitar fator K para reduzir risco oculto. | BR-006 | Alta |
| 6   | Validação de CPF          | Migrar | Regras com exceções legadas obrigatórias. | BR-009, BR-015 | Alta |
| 7   | Relatórios                | Evoluir | Corrigir visibilidade (incluindo eventos de exclusão). | BR-010 | Média |
| 8   | Auditoria                 | Migrar | Exigência de conformidade e rastreabilidade. | BR-010 | Alta |
| 9   | Gestão de Usuários        | Evoluir | Legado não cobre API moderna; adequar a segurança atual. | [GREENFIELD] | Média |
| 10  | Conciliação Financeira    | Migrar | Fechamento financeiro e status de pagamento. | BR-006 | Alta |
| 11  | Fator K (governança)      | Evoluir | Tornar regra explícita/parametrizável e auditável no moderno. | BR-006 | Alta |
| 12  | Integração Banco Real     | Descartar | Integração legada descontinuada e fora do valor atual. | [LEGADO OBSOLETO] | Baixa |

> Adicione linhas para cada funcionalidade identificada no `discovery-report.md` do Estágio 1.

---

## Funcionalidades Novas (não existem no legado)

> Liste funcionalidades que o SIFAP 2.0 deveria ter e que não existem no sistema legado. Cada uma vira REQ-ID com `source_legacy: [GREENFIELD] <justificativa>`.

| #   | Funcionalidade Nova | Justificativa | Prioridade | Complexidade |
| --- | ------------------- | ------------- | ---------- | ------------ |
| N1  | Autenticação JWT/OAuth2 para API | Legado por sessão terminal não atende arquitetura moderna de APIs. | Alta | Média |
| N2  | Trilha de auditoria API com consulta estruturada | Garantir visibilidade completa e conformidade regulatória. | Alta | Média |
| N3  | Parametrização explícita do fator K | Reduz risco de regra implícita não documentada no cálculo. | Alta | Alta |

---

## Resumo de Escopo

| Decisão   | Quantidade | Percentual |
| --------- | ---------- | ---------- |
| Migrar    | 5          | 41,7%      |
| Descartar | 1          | 8,3%       |
| Evoluir   | 6          | 50,0%      |
| **Total** |            | 100%       |

## Riscos de Escopo

> Liste os riscos das decisões tomadas:

| Risco | Probabilidade        | Impacto              | Mitigação |
| ----- | -------------------- | -------------------- | --------- |
| Fator K não documentado gerar divergência de cálculo | Alta | Alto | Criar requisito dedicado (REQ-ADM-004) com regra explícita e validação com Par 1/Par 2 antes da implementação. |
| Ambiguidade entre valor base ajustado e reajuste anual | Média | Alto | Definir ordem de cálculo na spec e validar com testes de regressão do legado. |
| Escopo exceder janela do Estágio 3 | Média | Médio | Priorizar P0 e postergar evoluções não críticas para backlog. |

## Aprovação

- [x] Par 1 (Product Owner) aprovou as decisões de escopo
- [ ] Par 2 (Enterprise Architect) validou a viabilidade técnica
- [ ] Par 3 (Technical Lead) confirmou que cabe nas 3 horas do Estágio 3
- [ ] Time concordou com as prioridades

**Assinatura PO (Par 1):** Aprovado em 20/05/2026 com condicionante de formalização do fator K em REQ dedicado antes do início da implementação.

> **Aprovação obrigatória na Passagem #2** (~16:00). Sem ela, o Estágio 3 não começa.

— Paula


---

### Continuar a leitura

<table width="100%">
<tr>
<td width="50%" valign="top" align="left">
<sub><strong>← ANTERIOR</strong></sub><br/>
<a href="GUIDE.md"><strong>GUIDE do Estágio 2</strong></a><br/>
<sub>Passo a passo do estágio.</sub>
</td>
<td width="50%" valign="top" align="right">
<sub><strong>PRÓXIMO →</strong></sub><br/>
<a href="ADR-TEMPLATE.md"><strong>ADR-TEMPLATE</strong></a><br/>
<sub>Template de ADR.</sub>
</td>
</tr>
</table>

<sub>↑ <a href="../README.md">Voltar ao Kit PT-BR</a></sub>

