<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Mapa de Dependências — SIFAP Legado

![ESTÁGIO 01 Arqueologia](https://img.shields.io/badge/ESTÁGIO-01%20Arqueologia-F25022?style=for-the-badge) ![TIPO Worksheet](https://img.shields.io/badge/TIPO-Worksheet-1A1A1A?style=for-the-badge) ![PREENCHA Durante S1](https://img.shields.io/badge/PREENCHA-Durante%20S1-737373?style=for-the-badge)

> 🗺 **Você está aqui:** [Kit PT-BR](../README.md) → [Estágio 1](README.md) → **dependency-map**

> **Para quem é isto?** Este é um **artefato preenchido pelo time** durante o Estágio 1 (Arqueologia).
>
> **O que você terá ao final do estágio:**
>
> 1. Este documento totalmente preenchido com os dados reais do legado SIFAP
> 2. Rastreabilidade para `01-arqueologia/legado-sifap/` (programas `.NSN` e DDMs)
> 3. Base de evidência usada nas EARS do Estágio 2 (`source_legacy:`)
>
> 📘 **Guia passo a passo:** [`GUIDE.md`](GUIDE.md).


> Use diagramas Mermaid para mapear as dependências entre programas Natural e DDMs Adabas.
> O objetivo é visualizar "quem chama quem" e "quem lê/escreve o quê".

## Como descobrir dependências

- Use `grep` ou Copilot Chat para listar todas as ocorrências de `CALLNAT` nos 15 arquivos `.NSN`.
- Prompt útil: _"Liste todas as ocorrências de CALLNAT nestes arquivos e desenhe um diagrama Mermaid."_
- Para leitura/escrita em DDMs: procure por `READ`, `READ LOGICAL`, `STORE`, `UPDATE`, `DELETE`.

## Diagrama de Dependências entre Programas

> Substitua o exemplo abaixo pelo mapa real do seu time. **Meta:** cobrir todos os 15 programas, sem órfãos.

```mermaid
flowchart TD
 subgraph "Cadastro e Validação"
 CADBENEF["CADBENEF.NSN"]
 CADDEPEND["CADDEPEND.NSN"]
 CADPROG["CADPROG.NSN"]
 VALBENEF["VALBENEF.NSN"]
 VALDOCS["VALDOCS.NSN"]
 VALELEG["VALELEG.NSN"]
 end

 subgraph "Cálculo"
 CALCBENF["CALCBENF.NSN"]
 CALCDSCT["CALCDSCT.NSN"]
 CALCCORR["CALCCORR.NSN"]
 end

 subgraph "Batch"
 BATCHPGT["BATCHPGT.NSN"]
 BATCHCON["BATCHCON.NSN"]
 BATCHREL["BATCHREL.NSN"]
 end

 subgraph "Consulta e Relatórios"
 CONSBENF["CONSBENF.NSN"]
 RELPGT["RELPGT.NSN"]
 RELAUDIT["RELAUDIT.NSN"]
 end

 subgraph "DDMs Adabas"
 DDM_BENEF[("BENEFICIARIO.ddm")]
 DDM_PAG[("PAGAMENTO.ddm")]
 DDM_PROG[("PROGRAMA-SOCIAL.ddm")]
 DDM_AUD[("AUDITORIA.ddm")]
 end

 CADBENEF --> DDM_BENEF
 CADDEPEND --> DDM_BENEF
 VALBENEF --> DDM_BENEF
 VALDOCS --> DDM_BENEF
 CONSBENF --> DDM_BENEF
 RELPGT --> DDM_BENEF

 CADPROG --> DDM_PROG
 VALELEG --> DDM_PROG

 CALCBENF --> DDM_BENEF
 CALCBENF --> DDM_PROG
 CALCBENF --> DDM_PAG
 CALCDSCT --> DDM_BENEF
 CALCDSCT --> DDM_PAG
 CALCCORR --> DDM_PAG

 BATCHPGT --> DDM_BENEF
 BATCHPGT --> DDM_PROG
 BATCHPGT --> DDM_PAG
 BATCHCON --> DDM_PAG
 BATCHCON --> DDM_AUD
 BATCHREL --> DDM_PAG
 BATCHREL --> DDM_BENEF
 RELAUDIT --> DDM_AUD

 BATCHPGT -. ordem operacional .-> BATCHCON
 BATCHCON -. ordem operacional .-> BATCHREL
```

> **Instrução:** este é apenas um exemplo inicial com 6 programas.
> Seu time deve mapear **todos os 15 programas** e os **4 DDMs**.

## Diagrama de Fluxo de Dados (DDMs)

```mermaid
flowchart LR
 subgraph "Entradas"
 UI["Terminal Natural"]
 RET["Arquivo CNAB 240"]
 end

 subgraph "Processamento"
 CAD["Cadastro/Validação"]
 CALC["Cálculo"]
 BAT["Batch"]
 REP["Consultas/Relatórios"]
 end

 subgraph "Armazenamento Adabas"
 DDM1[("BENEFICIARIO")]
 DDM2[("PAGAMENTO")]
 DDM3[("PROGRAMA-SOCIAL")]
 DDM4[("AUDITORIA")]
 end

 UI --> CAD
 CAD --> CALC
 CALC --> BAT
 RET --> BAT
 BAT --> REP

 CAD <--> DDM1
 CAD <--> DDM3
 CALC <--> DDM1
 CALC <--> DDM2
 CALC <--> DDM3
 BAT <--> DDM2
 BAT <--> DDM4
 REP <--> DDM1
 REP <--> DDM2
 REP <--> DDM4
```

> Substitua "DDM 3: ???" e "DDM 4: ???" pelos nomes reais encontrados em [`../01-arqueologia/legado-sifap/adabas-ddms/`](../01-arqueologia/legado-sifap/adabas-ddms/).

## Tabela de Dependências

| Programa     | Chama (CALLNAT) | Lê (READ) DDMs | Escreve (STORE/UPDATE) DDMs | Observações |
| ------------ | --------------- | -------------- | --------------------------- | ----------- |
| BATCHCON.NSN | Nenhum | `PAGAMENTO`, `AUDITORIA` | `PAGAMENTO`, `AUDITORIA` | Concilia retorno bancário e grava trilha de auditoria. |
| BATCHPGT.NSN | Nenhum | `BENEFICIARIO`, `PROGRAMA-SOCIAL`, `PAGAMENTO` | `PAGAMENTO` | Batch crítico mensal; ordenação por CPF é dependência downstream. |
| BATCHREL.NSN | Nenhum | `PAGAMENTO`, `BENEFICIARIO` | — | Consolida relatórios por região/status. |
| CADBENEF.NSN | Nenhum | `BENEFICIARIO` | `BENEFICIARIO` | Inclusão/alteração de beneficiário com validações básicas. |
| CADDEPEND.NSN | Nenhum | `BENEFICIARIO` | `BENEFICIARIO` | Manipula grupo periódico de dependentes (PE). |
| CADPROG.NSN | Nenhum | `PROGRAMA-SOCIAL` | `PROGRAMA-SOCIAL` | Cadastro de programas sociais e cálculo de valor ajustado. |
| CALCBENF.NSN | Nenhum | `BENEFICIARIO`, `PROGRAMA-SOCIAL` | `PAGAMENTO` | Cálculo principal de benefício mensal. |
| CALCCORR.NSN | Nenhum | `PAGAMENTO` | `PAGAMENTO` | Correção retroativa por índice IPCA. |
| CALCDSCT.NSN | Nenhum | `PAGAMENTO`, `BENEFICIARIO` | `PAGAMENTO` | Aplica descontos por tipo e teto parcial. |
| CONSBENF.NSN | Nenhum | `BENEFICIARIO`, `PAGAMENTO` | — | Consulta operacional de beneficiário e pagamentos. |
| RELAUDIT.NSN | Nenhum | `AUDITORIA` | — | Relatório de trilha com filtros (exclui ação `EX`). |
| RELPGT.NSN | Nenhum | `PAGAMENTO`, `BENEFICIARIO` | — | Relatório de pagamentos por período. |
| VALBENEF.NSN | Nenhum | — | — | Rotina de validação de domínio (sem persistência direta). |
| VALDOCS.NSN | Nenhum | — | — | Validação documental e regra de prefixos especiais. |
| VALELEG.NSN | Nenhum | `BENEFICIARIO`, `PROGRAMA-SOCIAL` | — | Determina elegibilidade por regras de status, renda, idade e região. |

## Dependências Circulares

> Liste aqui qualquer dependência circular encontrada (programa A chama B que chama A):

- Nenhuma encontrada até agora.
- Não há ciclos de `CALLNAT` (nenhum `CALLNAT` foi encontrado nos 15 programas).
- O acoplamento ocorre principalmente por leitura/escrita compartilhada em `PAGAMENTO` e `BENEFICIARIO`.

## Programas Órfãos

> Programas que não são chamados por nenhum outro (possíveis pontos de entrada ou código morto):

- A investigar.
- Sem `CALLNAT`, todos os programas funcionam como pontos de entrada independentes.
- Dependências reais são de dados: sequência operacional observada `BATCHPGT` → `BATCHCON` → `BATCHREL`.

---

### Continuar a leitura

<table width="100%">
<tr>
<td width="50%" valign="top" align="left">
<sub><strong>← ANTERIOR</strong></sub><br/>
<a href="business-rules-catalog.md"><strong>business-rules-catalog.md</strong></a><br/>
<sub>Catálogo de regras.</sub>
</td>
<td width="50%" valign="top" align="right">
<sub><strong>PRÓXIMO →</strong></sub><br/>
<a href="discovery-report.md"><strong>discovery-report.md</strong></a><br/>
<sub>Síntese final.</sub>
</td>
</tr>
</table>

<sub>↑ <a href="README.md">Voltar ao Kit PT-BR</a></sub>

