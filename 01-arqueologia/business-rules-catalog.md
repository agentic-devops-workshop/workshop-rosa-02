<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Catálogo de Regras de Negócio — SIFAP Legado

![ESTÁGIO 01 Arqueologia](https://img.shields.io/badge/ESTÁGIO-01%20Arqueologia-F25022?style=for-the-badge) ![TIPO Worksheet](https://img.shields.io/badge/TIPO-Worksheet-1A1A1A?style=for-the-badge) ![PREENCHA Durante S1](https://img.shields.io/badge/PREENCHA-Durante%20S1-737373?style=for-the-badge)

> 🗺 **Você está aqui:** [Kit PT-BR](../README.md) → [Estágio 1](README.md) → **business-rules-catalog**

> **Para quem é isto?** Este é um **artefato preenchido pelo time** durante o Estágio 1 (Arqueologia).
>
> **O que você terá ao final do estágio:**
>
> 1. Este documento totalmente preenchido com os dados reais do legado SIFAP
> 2. Rastreabilidade para `01-arqueologia/legado-sifap/` (programas `.NSN` e DDMs)
> 3. Base de evidência usada nas EARS do Estágio 2 (`source_legacy:`)
>
> 📘 **Guia passo a passo:** [`GUIDE.md`](GUIDE.md).


> Registre aqui todas as regras de negócio extraídas do código Natural/Adabas.
> Cada regra precisa ter rastreabilidade até o código-fonte.
>
> **REGRA DURA:** linhas com `Programa Fonte` vazio são **inválidas** e não contam para o gate do Estágio 2. Use o formato `01-arqueologia/legado-sifap/natural-programs/ARQUIVO.NSN#L<inicio>-L<fim>` sempre que possível. Mínimo aceito: nome do arquivo .NSN.

## Como pensar em "regra de negócio"

O que conta:

- Um `IF` que decide algo no domínio (ex.: _"se a UF é do Nordeste e o programa é Seca, valor base × 1.2"_)
- Uma constante numérica sem explicação (ex.: `0.075` num cálculo de imposto)
- Uma transição de status com regra (ex.: _"só de A para S, nunca de I para A"_)
- Um tratamento especial para um caso (ex.: _"se o CPF começa com 999, é teste"_)

O que NÃO conta: paginação de relatório, formatação de saída, manipulação de cursor Adabas, abertura de arquivo. Ignore esses detalhes de implementação.

## Níveis de Risco

| Nível       | Descrição                                                     |
| ----------- | ------------------------------------------------------------- |
| **CRÍTICO** | Regra financeira ou de segurança — erro causa prejuízo direto |
| **ALTO**    | Regra de negócio central — afeta fluxo principal              |
| **MÉDIO**   | Regra de validação ou formatação — afeta qualidade dos dados  |
| **BAIXO**   | Regra de apresentação ou conveniência — impacto limitado      |

## Regras Encontradas

| ID     | Regra de Negócio | Programa Fonte | Campos DDM | Nível de Risco | Notas |
| ------ | ---------------- | -------------- | ---------- | -------------- | ----- |
| BR-001 | Na inclusão (`OPER = I`), se já existir beneficiário com o mesmo CPF, a operação deve ser rejeitada. | `01-arqueologia/legado-sifap/natural-programs/CADBENEF.NSN#L143-L147` | `BENEFICIARIO.CPF`, `BENEFICIARIO.STATUS` | ALTO | Evita duplicidade cadastral no arquivo principal de beneficiários. |
| BR-002 | Beneficiários com idade acima de 75 anos devem ter status ajustado para `S`, mesmo após definição inicial de status `A`. | `01-arqueologia/legado-sifap/natural-programs/CADBENEF.NSN#L163-L168` | `BENEFICIARIO.DT-NASCIMENTO`, `BENEFICIARIO.STATUS` | ALTO | Regra introduzida por ajuste de status idoso (anotação de alteração no cabeçalho do programa). |
| BR-003 | Não é permitida inclusão de dependente para beneficiário com status `C` (cancelado) ou `D` (desligado). | `01-arqueologia/legado-sifap/natural-programs/CADDEPEND.NSN#L56-L59` | `BENEFICIARIO.STATUS`, `BENEFICIARIO.NUM-DEPENDENTES` | ALTO | Bloqueio de negócio antes do loop de inclusão de dependentes. |
| BR-004 | Cada beneficiário pode ter no máximo 5 dependentes cadastrados; ao exceder, a inclusão deve ser interrompida. | `01-arqueologia/legado-sifap/natural-programs/CADDEPEND.NSN#L63-L66` | `BENEFICIARIO.NUM-DEPENDENTES`, `BENEFICIARIO.DEPENDENTES(PE)` | ALTO | Limite explícito de cardinalidade no grupo periódico de dependentes. |
| BR-005 | Não é permitido cadastrar programa social com `COD-PROGRAMA` já existente. | `01-arqueologia/legado-sifap/natural-programs/CADPROG.NSN#L82-L84` | `PROGRAMA-SOCIAL.COD-PROGRAMA`, `PROGRAMA-SOCIAL.STATUS-PROG` | ALTO | Garante unicidade funcional do identificador do programa. |
| BR-006 | O valor base persistido do programa deve ser recalculado por fator de reajuste: `VLR-CALC = VLR-BASE * (1 + FATOR-REAJ * 0.347215)`. | `01-arqueologia/legado-sifap/natural-programs/CADPROG.NSN#L87-L88` | `PROGRAMA-SOCIAL.VLR-BASE`, `PROGRAMA-SOCIAL.FATOR-REAJUSTE` | CRÍTICO | Regra de cálculo financeiro aplicada antes de gravar o programa. |
| BR-007 | Quando UF é informada, ela deve existir na tabela de 27 UFs válidas; caso contrário, a validação cadastral falha. | `01-arqueologia/legado-sifap/natural-programs/VALBENEF.NSN#L145-L159` | `BENEFICIARIO.UF`, `BENEFICIARIO.STATUS` | MÉDIO | Regra de consistência territorial aplicada no cadastro do beneficiário. |
| BR-008 | O status do beneficiário só pode assumir os valores `A`, `S`, `C`, `I` ou `D`; qualquer outro status torna o cadastro inválido. | `01-arqueologia/legado-sifap/natural-programs/VALBENEF.NSN#L164-L169` | `BENEFICIARIO.STATUS`, `BENEFICIARIO.CPF` | ALTO | Enumeração de status de domínio usada em múltiplos fluxos de validação. |
| BR-009 | CPF com todos os dígitos iguais é inválido, exceto quando inicia com `000`, que é tratado como exceção válida de teste governamental. | `01-arqueologia/legado-sifap/natural-programs/VALBENEF.NSN#L195-L202` | `BENEFICIARIO.CPF`, `BENEFICIARIO.STATUS` | ALTO | Exceção explícita do legado com impacto direto na validação documental. |
| BR-010 | Beneficiário da região `99` é automaticamente elegível e encerra a validação de elegibilidade como caso especial. | `01-arqueologia/legado-sifap/natural-programs/VALELEG.NSN#L107-L111` | `BENEFICIARIO.COD-REGIAO`, `BENEFICIARIO.COD-PROGRAMA` | ALTO | Regra especial de negócio para região internacional/diplomática. |
| BR-011 | Programa social inativo (`STATUS-PROG != A`) não pode ser usado em validação de elegibilidade. | `01-arqueologia/legado-sifap/natural-programs/VALELEG.NSN#L99-L102` | `PROGRAMA-SOCIAL.STATUS-PROG`, `PROGRAMA-SOCIAL.COD-PROGRAMA` | ALTO | Bloqueio preventivo antes de avaliar critérios de elegibilidade do beneficiário. |
| BR-012 | Para programas previdenciários (`TIPO = P`), o beneficiário deve ter idade mínima de 60 anos. | `01-arqueologia/legado-sifap/natural-programs/VALELEG.NSN#L183-L189` | `PROGRAMA-SOCIAL.TIPO`, `BENEFICIARIO.DT-NASCIMENTO` | ALTO | Regra etária específica por tipo de programa. |
| BR-013 | Para programas de trabalho (`TIPO = T`), a idade elegível deve permanecer entre 16 e 65 anos. | `01-arqueologia/legado-sifap/natural-programs/VALELEG.NSN#L190-L196` | `PROGRAMA-SOCIAL.TIPO`, `BENEFICIARIO.DT-NASCIMENTO` | ALTO | Faixa etária obrigatória para concessão em programas laborais. |
| BR-014 | RG informado deve possuir no mínimo 5 caracteres úteis; abaixo disso, o documento é inválido. | `01-arqueologia/legado-sifap/natural-programs/VALDOCS.NSN#L152-L162` | `BENEFICIARIO.RG`, `BENEFICIARIO.DOCUMENTOS-OK` | MÉDIO | Regra de qualidade mínima para documento civil no fluxo de validação. |
| BR-015 | Se o CPF tiver prefixo especial cadastrado (`000`, `001`, `002`, `010`, `011`, `099`, `100`, `999`), o sistema marca documento especial como válido e limpa erros da validação. | `01-arqueologia/legado-sifap/natural-programs/VALDOCS.NSN#L168-L179` | `BENEFICIARIO.CPF`, `BENEFICIARIO.DOCUMENTOS-OK` | ALTO | Exceção de governança/teste que sobrescreve resultado padrão de validação documental. |

> Adicione mais linhas conforme necessário. Lembre-se: existem **10 regras escondidas** no código!

## Exemplo de linha bem preenchida

| ID     | Regra de Negócio                                                                        | Programa Fonte                                   | Campos DDM                                                               | Nível de Risco | Notas                                      |
| ------ | --------------------------------------------------------------------------------------- | ------------------------------------------------ | ------------------------------------------------------------------------ | -------------- | ------------------------------------------ |
| BR-EX-001 | Desconto total não pode exceder 30% do valor bruto, exceto descontos judiciais (tipo J) | `01-arqueologia/legado-sifap/natural-programs/CALCDSCT.NSN#L142-L148` | `PAGAMENTO.VLR-BRUTO`, `PAGAMENTO.VLR-TOTAL-DSCT`, `PAGAMENTO.TIPO-DSCT` | CRÍTICO        | Exemplo ilustrativo. Não reutilizar IDs reais do catálogo principal. |

## Regras por Categoria

### Cálculos Financeiros

<!-- Liste aqui as regras relacionadas a cálculos de valores, benefícios, etc. -->

### Validações de Status

<!-- Liste aqui as regras de transição de status (A, S, C, I, D) -->

### Regras de Autorização

<!-- Liste aqui as regras de quem pode fazer o quê -->

### Regras de Negócio Temporais

<!-- Liste aqui regras com prazos, datas-limite, períodos -->

## Resumo Estatístico

- Total de regras encontradas: 15
- Regras críticas: 1
- Regras com duplicação: 0
- Regras sem documentação (escondidas): 3

## Nota de Governança (Par 1)

- Antes de derivar novos `REQ-IDs` no Estágio 2, o Par 1 deve validar que não há colisão de IDs no catálogo (`BR-XXX`) e que exemplos não reutilizam IDs reais.
- Qualquer ajuste de identificação de regra deve ser registrado primeiro no catálogo e só então propagado para a spec moderna.

---

### Continuar a leitura

<table width="100%">
<tr>
<td width="50%" valign="top" align="left">
<sub><strong>← ANTERIOR</strong></sub><br/>
<a href="GUIDE.md"><strong>GUIDE do Estágio 1</strong></a><br/>
<sub>Passo a passo do estágio.</sub>
</td>
<td width="50%" valign="top" align="right">
<sub><strong>PRÓXIMO →</strong></sub><br/>
<a href="dependency-map.md"><strong>dependency-map.md</strong></a><br/>
<sub>Mapa de quem chama quem.</sub>
</td>
</tr>
</table>

<sub>↑ <a href="README.md">Voltar ao Kit PT-BR</a></sub>

