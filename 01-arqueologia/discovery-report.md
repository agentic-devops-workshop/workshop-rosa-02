<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Relatório de Descoberta — Estágio 1: Arqueologia Digital

![ESTÁGIO 01 Arqueologia](https://img.shields.io/badge/ESTÁGIO-01%20Arqueologia-F25022?style=for-the-badge) ![TIPO Worksheet](https://img.shields.io/badge/TIPO-Worksheet-1A1A1A?style=for-the-badge) ![PREENCHA Durante S1](https://img.shields.io/badge/PREENCHA-Durante%20S1-737373?style=for-the-badge)

> 🗺 **Você está aqui:** [Kit PT-BR](../README.md) → [Estágio 1](README.md) → **discovery-report**

> **Para quem é isto?** Este é um **artefato preenchido pelo time** durante o Estágio 1 (Arqueologia).
>
> **O que você terá ao final do estágio:**
>
> 1. Este documento totalmente preenchido com os dados reais do legado SIFAP
> 2. Rastreabilidade para `01-arqueologia/legado-sifap/` (programas `.NSN` e DDMs)
> 3. Base de evidência usada nas EARS do Estágio 2 (`source_legacy:`)
>
> 📘 **Guia passo a passo:** [`GUIDE.md`](GUIDE.md).


> Este documento consolida todas as descobertas do Estágio 1.
> Preencha cada seção com as conclusões do time. **Este é o input principal do Estágio 2** — sem ele, a especificação vira chute.

**Time**: workshop-rosa-02
**Data**: 20/05/2026
**Edição**: Dia 2 — Estágio 1 (Arqueologia)
**Participantes**: Par 1 (Product Owner + Requirements Engineer) com apoio dos pares 2–5

---

## 1. Sumário Executivo

> Em 3 a 5 frases, resuma o que o time descobriu sobre o SIFAP legado.
> O que é este sistema? Qual sua criticidade? Qual o estado do código?

O SIFAP legado é um sistema Natural/Adabas altamente orientado a regras de negócio e processamento em lote para pagamentos sociais. A criticidade é alta porque regras de elegibilidade, cálculo e conciliação estão espalhadas em 15 programas `.NSN` sem centralização formal. O código mostra dependências fortes por dados (DDMs compartilhados), mesmo sem `CALLNAT` explícito entre programas. Foram identificadas exceções históricas e constantes não documentadas que podem causar regressões financeiras e funcionais se não forem preservadas na modernização.

---

## 2. Visão Geral do Sistema

### 2.1 Propósito do SIFAP

Gerencia ciclo completo de benefícios sociais: cadastro de beneficiários e dependentes, cadastro de programas, validações documentais/elegibilidade, cálculo de pagamentos, descontos, correções retroativas, conciliação bancária e relatórios operacionais/auditoria.

### 2.2 Arquitetura Legada

Arquitetura legada em Natural com persistência Adabas, composta por 15 programas e 4 DDMs (`BENEFICIARIO`, `PAGAMENTO`, `PROGRAMA-SOCIAL`, `AUDITORIA`). Não há cadeia de chamadas `CALLNAT`; o acoplamento principal ocorre por leitura/escrita compartilhada dos DDMs. Fluxo operacional dominante: `BATCHPGT` (geração) → `BATCHCON` (conciliação) → `BATCHREL` (consolidação relatório).

### 2.3 Usuários e Perfis

Perfis inferidos pelo código: operação de cadastro (atendimento), processamento batch financeiro, equipe de conciliação bancária, auditoria/controle interno e consumidores de relatórios de gestão.

---

## 3. Principais Descobertas

### 3.1 Regras de Negócio Críticas

> Liste as 5 regras de negócio mais importantes encontradas.

1. Limite de dependentes por beneficiário no cadastro operacional (`BR-004`).
2. Reajuste de status para idosos no momento da inclusão (`BR-002`).
3. Cálculo de benefício com fatores regionais/familiares/renda/idade (`BR-006` + evidências em `CALCBENF.NSN`).
4. Elegibilidade automática para região especial `99` (`BR-010`).
5. Desconto judicial fora do teto geral de 30% (`BR-006` + mistério `MYS-006`).

### 3.2 Dependências Complexas

> Quais programas estão mais acoplados? Onde há risco de efeito cascata?

Maior acoplamento em `PAGAMENTO` e `BENEFICIARIO`: programas de cálculo, batch, consulta e relatório compartilham os mesmos registros. Risco de efeito cascata alto em mudanças de status, arredondamento e campos monetários, porque afetam cálculo, conciliação e auditoria simultaneamente.

### 3.3 Dívida Técnica Identificada

> Que problemas no código legado vão complicar a migração?

- [x] Regras críticas dependem de constantes mágicas sem documentação (`0.347215`, `0.30`, fatores regionais).
- [x] Estratégias de arredondamento/truncamento não uniformes entre programas.
- [x] Exceções históricas no código (prefixos especiais, região 99, blocos legados comentados).

### 3.4 Gaps de Documentação

> O que a documentação existente NÃO cobre?

A documentação funcional não cobre exceções de domínio encontradas no código (ex.: região 99, filtro de exclusões em auditoria, regras específicas de dezembro). Também não explica origem de constantes financeiras e dependências operacionais por ordenação no batch.

---

## 4. Mistérios e Riscos

### 4.1 Mistérios Não Resolvidos

> Resuma os mistérios do arquivo `mysteries-found.md` que permanecem sem explicação.

| ID  | Descrição | Risco para Migração |
| --- | --------- | ------------------- |
| MYS-003 | Constante de reajuste sem fonte documental (`0.347215`) | Erro em cálculo base de programas sociais |
| MYS-005 | Truncamento sistemático em correção retroativa | Divergência acumulada de centavos |
| MYS-006 | Desconto judicial fora do teto geral | Regressão jurídica/financeira |
| MYS-008 | Elegibilidade automática da região 99 | Quebra de regra especial de domínio |
| MYS-010 | Ação `EX` ocultada em relatório de auditoria | Perda de rastreabilidade e compliance |

### 4.2 Riscos para o Estágio 2

> O que o time de especificação precisa saber antes de começar?

1. Preservar exceções explícitas de legado antes de generalizar regras (idade, região, prefixos especiais).
2. Definir política única de arredondamento para evitar divergência entre cálculo, correção e relatório.
3. Garantir rastreabilidade requisito↔evidência (`source_legacy`) para todas as EARS do Estágio 2.

---

## 5. Recomendações

### 5.1 O que migrar primeiro

> Com base na priorização do Par 1 (Product Owner), quais funcionalidades devem ser migradas primeiro?

| Prioridade | Funcionalidade | Justificativa |
| ---------- | -------------- | ------------- |
| 1          | Geração mensal de pagamentos (`BATCHPGT` + `CALCBENF`) | Núcleo de valor do sistema; impacto direto no beneficiário. |
| 2          | Elegibilidade e validações (`VALELEG`, `VALBENEF`, `VALDOCS`) | Evita concessão indevida e preserva regras de conformidade. |
| 3          | Conciliação e auditoria (`BATCHCON`, `RELAUDIT`) | Garante fechamento financeiro e trilha de controle. |

### 5.2 O que descartar

> Funcionalidades que provavelmente não precisam ser migradas:

- Suporte a integração legada do Banco Real comentada em código: tecnologia descontinuada e sem uso operacional.
- Campos de biometria marcados como "não implementado" no DDM (`HASH-DIGITAL`): manter fora do v1.

### 5.3 O que evoluir

> Funcionalidades que devem ser migradas E melhoradas:

- Relatórios de auditoria: remover ocultação de eventos de exclusão ou torná-la parametrizável.
- Política de arredondamento: padronizar entre cálculo mensal, correção retroativa e consolidação.
- Regras especiais (região 99/prefixos): externalizar em configuração versionada e auditável.

---

## 6. Métricas do Estágio

| Métrica                       | Valor        |
| ----------------------------- | ------------ |
| Programas analisados          | 15 / 15       |
| DDMs mapeados                 | 4 / 4         |
| Regras de negócio encontradas | 15            |
| Regras escondidas encontradas | 10 / 10       |
| Easter eggs encontrados       | 3 / 3         |
| Termos no glossário           | 30            |
| Mistérios catalogados         | 10            |
| Tempo total gasto             | 3.5 horas     |

---

## 7. Notas para o Próximo Estágio

> Deixe aqui mensagens para o time no Estágio 2 (Especificação Moderna):

Para o Estágio 2, priorizar requisitos EARS vinculados ao fluxo de pagamento mensal e às exceções mapeadas em `mysteries-found.md`. Criar REQ-IDs específicos para arredondamento, descontos judiciais, região especial 99 e filtros de auditoria. Evitar decisões arquiteturais sem preservar a sequência operacional de batch e os estados de pagamento já usados por conciliação.

---

## Definição de Pronto deste relatório

- [x] Todas as seções acima preenchidas (sem placeholders).
- [x] Pelo menos 5 regras críticas listadas em §3.1, cada uma referenciando uma `BR-XXX` do catálogo.
- [x] Decisões de migrar/descartar/evoluir em §5 cobrem as 8+ funcionalidades principais.
- [x] Métricas de §6 conferem com os outros artefatos (glossary.md, business-rules-catalog.md, mysteries-found.md).

— Paula


---

### Continuar a leitura

<table width="100%">
<tr>
<td width="50%" valign="top" align="left">
<sub><strong>← ANTERIOR</strong></sub><br/>
<a href="mysteries-found.md"><strong>mysteries-found.md</strong></a><br/>
<sub>Lista de mistérios.</sub>
</td>
<td width="50%" valign="top" align="right">
<sub><strong>PRÓXIMO →</strong></sub><br/>
<a href="../02-spec-moderna/GUIDE.md"><strong>Estágio 2 — Spec</strong></a><br/>
<sub>Próximo estágio: spec moderna.</sub>
</td>
</tr>
</table>

<sub>↑ <a href="../README.md">Voltar ao Kit PT-BR</a></sub>

