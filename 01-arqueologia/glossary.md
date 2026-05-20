<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Glossário do SIFAP Legado

![ESTÁGIO 01 Arqueologia](https://img.shields.io/badge/ESTÁGIO-01%20Arqueologia-F25022?style=for-the-badge) ![TIPO Worksheet](https://img.shields.io/badge/TIPO-Worksheet-1A1A1A?style=for-the-badge) ![PREENCHA Durante S1](https://img.shields.io/badge/PREENCHA-Durante%20S1-737373?style=for-the-badge)

> 🗺 **Você está aqui:** [Kit PT-BR](../README.md) → [Estágio 1](README.md) → **glossary**

> **Para quem é isto?** Este é um **artefato preenchido pelo time** durante o Estágio 1 (Arqueologia).
>
> **O que você terá ao final do estágio:**
>
> 1. Este documento totalmente preenchido com os dados reais do legado SIFAP
> 2. Rastreabilidade para `01-arqueologia/legado-sifap/` (programas `.NSN` e DDMs)
> 3. Base de evidência usada nas EARS do Estágio 2 (`source_legacy:`)
>
> 📘 **Guia passo a passo:** [`GUIDE.md`](GUIDE.md).


> Preencha esta tabela com todos os termos, abreviações e siglas encontrados no código Natural/Adabas.
> **Meta: no mínimo 30 termos.**

## Por que isso importa

Sistemas legados têm vocabulário próprio que ninguém documenta em lugar nenhum — só está no nome das variáveis. Se o time do Estágio 2 não souber o que `DSCT`, `BENF`, `PE` ou `CTC` significam, vai escrever uma spec sobre o que ele _acha_ que isso significa. Glossário é o que evita esse desencontro.

## Como preencher

- **Termo**: a abreviação ou sigla exatamente como aparece no código
- **Expansão**: o significado completo do termo
- **Programa**: em qual arquivo `.NSN` ou `.ddm` o termo foi encontrado
- **Contexto**: breve explicação de como/onde o termo é usado

## Dica de extração

Prompt útil no Copilot Chat (cole o conteúdo de 2–3 arquivos `.NSN` no chat antes):

> _"Liste todas as abreviações e siglas usadas neste código Natural. Para cada uma, sugira a expansão e marque com 'CONFIRMADO' ou 'HIPÓTESE'."_

## Termos encontrados

| #   | Termo | Expansão | Programa | Contexto |
| --- | ----- | -------- | -------- | -------- |
| 1   | `CPF` | Cadastro de Pessoa Física | `CADBENEF.NSN`, `VALBENEF.NSN`, `VALDOCS.NSN` | Identificador principal do beneficiário e chave de busca. |
| 2   | `NIS` | Número de Identificação Social | `CADBENEF.NSN`, `VALELEG.NSN` | Exigido em regras específicas de elegibilidade. |
| 3   | `UF` | Unidade Federativa | `CADBENEF.NSN`, `VALBENEF.NSN` | Validada contra tabela fixa de 27 estados. |
| 4   | `CEP` | Código de Endereçamento Postal | `CADBENEF.NSN`, `VALBENEF.NSN` | Campo cadastral obrigatório para endereço. |
| 5   | `COD-REGIAO` | Código de região geográfica | `CADBENEF.NSN`, `CALCBENF.NSN`, `VALELEG.NSN` | Determina fatores regionais e exceções de elegibilidade. |
| 6   | `COMPETENCIA` | Referência de processamento `AAAAMM` | `CALCBENF.NSN`, `BATCHPGT.NSN`, `RELPGT.NSN` | Define mês/ano do pagamento processado. |
| 7   | `VLR-BRUTO` | Valor bruto do benefício | `CALCBENF.NSN`, `CALCDSCT.NSN`, `BATCHREL.NSN` | Base para descontos e totais de relatório. |
| 8   | `VLR-DESCONTO` | Soma de deduções aplicadas | `CALCDSCT.NSN`, `BATCHPGT.NSN`, `BATCHREL.NSN` | Subtraído do bruto para gerar líquido. |
| 9   | `VLR-LIQUIDO` | Valor líquido a pagar | `CALCBENF.NSN`, `BATCHCON.NSN`, `RELPGT.NSN` | Valor efetivo conciliado com retorno bancário. |
| 10  | `STATUS` | Situação cadastral do beneficiário | `CADBENEF.NSN`, `VALBENEF.NSN`, `VALELEG.NSN` | Estados `A/S/C/I/D` controlam elegibilidade e processamento. |
| 11  | `STATUS-PGTO` | Situação do pagamento | `BATCHCON.NSN`, `BATCHREL.NSN`, `RELPGT.NSN` | Estados de geração, pagamento, devolução e estorno. |
| 12  | `TIPO-PROG` | Tipo de programa social | `CADPROG.NSN`, `CALCBENF.NSN`, `VALELEG.NSN` | Define regras por domínio (`A`, `P`, `T`). |
| 13  | `TIPO-PGTO` | Tipo de pagamento | `CALCBENF.NSN`, `BATCHPGT.NSN` | Diferencia normal, décimo e terceiro. |
| 14  | `ABONO` | Valor adicional eventual | `CALCBENF.NSN`, `BATCHPGT.NSN` | Gratificação extra (ex.: abono natalino). |
| 15  | `13O` | Décimo terceiro benefício | `CALCBENF.NSN`, `BATCHPGT.NSN` | Parcela extra processada em dezembro. |
| 16  | `IPCA` | Índice de Preços ao Consumidor Amplo | `CALCCORR.NSN` | Índice mensal usado na correção retroativa. |
| 17  | `IND-CORRIGIDO` | Indicador de pagamento já corrigido | `CALCCORR.NSN` | Evita recalcular correção no mesmo registro. |
| 18  | `COD-ELEGIBILIDADE` | Código de regra específica de elegibilidade | `CADPROG.NSN`, `VALELEG.NSN` | Ativa validações adicionais (ex.: NIS/dependentes). |
| 19  | `PCT-DSCT` | Percentual de desconto | `CALCDSCT.NSN` | Alternativa ao valor fixo no cálculo de deduções. |
| 20  | `TIPO-DSCT` | Tipo de desconto | `CALCDSCT.NSN` | Classifica deduções (`J`, `P`, `I`, `S`, `A`). |
| 21  | `PE` | Periodic Group (grupo periódico) | `CADDEPEND.NSN`, `CALCDSCT.NSN`, `BENEFICIARIO.ddm` | Estrutura repetitiva para dependentes e descontos. |
| 22  | `MU` | Multiple Value (campo multivalorado) | `BENEFICIARIO.ddm` | Convenção Adabas para múltiplos valores por campo. |
| 23  | `DDM` | Data Definition Module | `adabas-ddms/*.ddm` | Define estrutura física/lógica dos arquivos Adabas. |
| 24  | `FDT` | Field Definition Table | `BENEFICIARIO.ddm` | Tabela técnica de campos/formatos do arquivo Adabas. |
| 25  | `CNAB 240` | Layout bancário de retorno/pagamento | `BATCHCON.NSN` | Formato de conciliação com arquivo de banco. |
| 26  | `COD-RETORNO` | Código de retorno bancário | `BATCHCON.NSN` | Determina atualização de status pós-conciliação. |
| 27  | `NUM-PAGTO` | Número sequencial do pagamento | `CALCBENF.NSN`, `BATCHCON.NSN` | Chave operacional para localizar transações. |
| 28  | `ACAO` | Tipo de evento de auditoria | `BATCHCON.NSN`, `RELAUDIT.NSN` | Classifica inclusão, alteração, conciliação e divergência. |
| 29  | `CHAVE-REF` | Chave de referência auditada | `BATCHCON.NSN`, `RELAUDIT.NSN` | Identifica objeto afetado no evento de auditoria. |
| 30  | `SIT-BENEFICIARIO` | Situação do benefício no DDM | `BENEFICIARIO.ddm`, `VALBENEF.NSN` | Mapeia estado de negócio persistido do beneficiário. |

> Adicione mais linhas conforme necessário. Não se limite a 30!

## Exemplo de linha bem preenchida

| #   | Termo  | Expansão | Programa                        | Contexto                                                                                                         |
| --- | ------ | -------- | ------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| 1   | `DSCT` | Desconto | `CALCDSCT.NSN`, `PAGAMENTO.ddm` | Tipo de dedução aplicada sobre valor bruto do pagamento. Tipos: 'J' (judicial), 'I' (imposto), 'T' (trabalhista) |

## Observações

- Anote aqui qualquer padrão de nomenclatura que o time identificou:
- Prefixo `VLR-` para valores monetários e `DT-` para datas.
- Prefixo `COD-` para chaves de domínio e tabelas de referência.
- Sufixo `-V` para views Natural ligadas aos DDMs.
- Convenções de prefixo/sufixo encontradas:
- Variáveis de trabalho locais com `#` (ex.: `#VLR-BRUTO`, `#COD-REG`).
- Campos de arquivo em caixa alta com hífen (ex.: `STATUS-PGTO`, `NUM-PAGTO`).
- Termos ambíguos que precisam de validação com especialista:
- Diferença operacional entre estados `I` (inativo) e `D` (desligado).
- Uso de códigos especiais de região (`99`) e sua governança.

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
<a href="business-rules-catalog.md"><strong>business-rules-catalog.md</strong></a><br/>
<sub>Catálogo de regras.</sub>
</td>
</tr>
</table>

<sub>↑ <a href="README.md">Voltar ao Kit PT-BR</a></sub>

