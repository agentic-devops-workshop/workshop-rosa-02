<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Mistérios Encontrados — SIFAP Legado

![ESTÁGIO 01 Arqueologia](https://img.shields.io/badge/ESTÁGIO-01%20Arqueologia-F25022?style=for-the-badge) ![TIPO Worksheet](https://img.shields.io/badge/TIPO-Worksheet-1A1A1A?style=for-the-badge) ![PREENCHA Durante S1](https://img.shields.io/badge/PREENCHA-Durante%20S1-737373?style=for-the-badge)

> 🗺 **Você está aqui:** [Kit PT-BR](../README.md) → [Estágio 1](README.md) → **mysteries-found**

> **Para quem é isto?** Este é um **artefato preenchido pelo time** durante o Estágio 1 (Arqueologia).
>
> **O que você terá ao final do estágio:**
>
> 1. Este documento totalmente preenchido com os dados reais do legado SIFAP
> 2. Rastreabilidade para `01-arqueologia/legado-sifap/` (programas `.NSN` e DDMs)
> 3. Base de evidência usada nas EARS do Estágio 2 (`source_legacy:`)
>
> 📘 **Guia passo a passo:** [`GUIDE.md`](GUIDE.md).


> Registre aqui toda lógica, comportamento ou código que o time não conseguiu explicar.
> "Mistérios" são trechos de código sem documentação, com lógica não-óbvia ou que parecem workarounds.
>
> **Cota mínima para passar pelo portão do Estágio 2:** 5 mistérios documentados.

## O que conta como "mistério"?

- Código que faz algo inesperado sem comentário explicando por quê
- Valores hardcoded sem explicação (números mágicos)
- Lógica condicional que parece um workaround ou gambiarra
- Campos no DDM que não são usados por nenhum programa
- Programas que existem mas não são chamados por ninguém
- Comportamento diferente entre o que a documentação diz e o que o código faz
- Easter eggs deixados pelos desenvolvedores originais

## Níveis de Confiança

| Nível     | Significado                                         |
| --------- | --------------------------------------------------- |
| **ALTA**  | Temos certeza de que há algo estranho aqui          |
| **MÉDIA** | Parece suspeito, mas pode ter explicação            |
| **BAIXA** | Pode ser intencional, mas não conseguimos confirmar |

## Mistérios Catalogados

| ID      | Descrição | Onde Encontrado | Impacto Potencial | Confiança |
| ------- | --------- | --------------- | ----------------- | --------- |
| MYS-001 | Status é alterado silenciosamente para `S` quando idade > 75, mesmo após status inicial `A`. | `01-arqueologia/legado-sifap/natural-programs/CADBENEF.NSN#L163-L168` | Mudança automática de status pode excluir beneficiário de fluxos futuros sem ação explícita do operador. | ALTA |
| MYS-002 | Código limita dependentes a 5, mas DDM define grupo periódico com até 10 ocorrências. | `01-arqueologia/legado-sifap/natural-programs/CADDEPEND.NSN#L63-L66`; `01-arqueologia/legado-sifap/adabas-ddms/BENEFICIARIO.ddm#L80-L87` | Inconsistência de regra de domínio vs. capacidade de dados; risco de perda de elegibilidade por regra restritiva. | ALTA |
| MYS-003 | Constante misteriosa `0.347215` compõe fator de reajuste sem origem documental. | `01-arqueologia/legado-sifap/natural-programs/CADPROG.NSN#L87-L88` | Erro de migração pode alterar cálculo base de programas e gerar efeito financeiro acumulado. | ALTA |
| MYS-004 | Em dezembro, o cálculo muda: adiciona 13º e abono natalino (15% para tipo `A`). | `01-arqueologia/legado-sifap/natural-programs/CALCBENF.NSN#L257-L274`; `BATCHPGT.NSN#L305-L318` | Se não replicado, pagamentos de dezembro divergem do legado (impacto social/financeiro alto). | ALTA |
| MYS-005 | Correção retroativa usa truncamento para 2 casas, causando perda sistemática de centavos. | `01-arqueologia/legado-sifap/natural-programs/CALCCORR.NSN#L170-L173` | Viés de arredondamento pode criar diferenças acumuladas na reconciliação histórica. | ALTA |
| MYS-006 | Desconto judicial (`J`) ignora teto de 30% aplicado aos demais tipos. | `01-arqueologia/legado-sifap/natural-programs/CALCDSCT.NSN#L136-L145`; `#L181-L186` | Regra legal diferenciada; tratar como bug causaria retenção indevida e passivo jurídico. | ALTA |
| MYS-007 | CPFs iniciados com `000` e prefixos especiais são aceitos sem validação documental completa. | `01-arqueologia/legado-sifap/natural-programs/VALBENEF.NSN#L195-L202`; `VALDOCS.NSN#L168-L179` | Pode ser backdoor de teste/governo; migração sem alinhamento pode quebrar fluxos administrativos. | MÉDIA |
| MYS-008 | Região `99` pula todas as verificações de elegibilidade e aprova automaticamente. | `01-arqueologia/legado-sifap/natural-programs/VALELEG.NSN#L107-L111` | Exceção crítica de domínio; omitir pode bloquear públicos especiais, manter sem controle pode gerar fraude. | ALTA |
| MYS-009 | Ordem do batch por CPF foi mantida por dependência de sistemas downstream. | `01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN#L198-L201` | Alterar ordenação no moderno pode quebrar conciliação e integração externa. | MÉDIA |
| MYS-010 | Eventos de auditoria com ação `EX` são filtrados e nunca exibidos no relatório. | `01-arqueologia/legado-sifap/natural-programs/RELAUDIT.NSN#L104-L111` | Ocultação sistemática de eventos reduz rastreabilidade e pode mascarar exclusões críticas. | ALTA |

## Detalhamento dos Mistérios

### MYS-001: Ajuste automático de status por idade

- **Arquivo**: `01-arqueologia/legado-sifap/natural-programs/CADBENEF.NSN#L163-L168`
- **Trecho de código**:

```natural
IF #OPER = 'I'
	MOVE 'A' TO #STATUS
END-IF

IF #IDADE > 75
	MOVE 'S' TO #STATUS
END-IF
```

- **O que esperávamos**: Status inicial permanecesse `A` até validação externa de regra social.
- **O que o código faz**: Sobrescreve para `S` de forma automática na inclusão para idade > 75.
- **Hipótese do time**: Regra histórica de controle manual para faixa etária específica.
- **Risco se ignorarmos**: Divergência de elegibilidade e suspensão indevida no sistema modernizado.

---

> Copie o bloco acima para cada mistério encontrado.

## Easter Eggs

> Dica: existem **3 easter eggs** escondidos no código legado. Registre aqui os que encontrar:

1. [x] Easter Egg 1: Prefixos especiais de CPF (`000`, `999`) que resetam erros e forçam validação positiva (`VALDOCS.NSN#L168-L179`).
2. [x] Easter Egg 2: Bloco comentado "Plano Verão" com multiplicadores de transição monetária (`CALCCORR.NSN#L102-L113`).
3. [x] Easter Egg 3: Código morto de integração com Banco Real descontinuado (`BATCHCON.NSN#L235-L250`).

## Resumo

- Total de mistérios encontrados: 10
- Confiança alta: 8
- Confiança média: 2
- Confiança baixa: 0
- Easter eggs encontrados: 3 / 3

---

### Continuar a leitura

<table width="100%">
<tr>
<td width="50%" valign="top" align="left">
<sub><strong>← ANTERIOR</strong></sub><br/>
<a href="mysteries-checklist.md"><strong>mysteries-checklist.md</strong></a><br/>
<sub>Lista do que procurar.</sub>
</td>
<td width="50%" valign="top" align="right">
<sub><strong>PRÓXIMO →</strong></sub><br/>
<a href="discovery-report.md"><strong>discovery-report.md</strong></a><br/>
<sub>Síntese final.</sub>
</td>
</tr>
</table>

<sub>↑ <a href="README.md">Voltar ao Kit PT-BR</a></sub>

