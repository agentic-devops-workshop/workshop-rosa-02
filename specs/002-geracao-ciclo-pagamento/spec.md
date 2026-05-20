<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Spec 002 — Geração de Ciclo de Pagamento

## Contexto
Esta spec transforma descobertas do Estágio 1 em requisitos EARS testáveis para o fluxo de geração mensal de pagamentos do SIFAP. O foco inicial cobre cálculo, elegibilidade e validações críticas que impactam pagamento, conformidade e conciliação.

## Escopo
- Inclusão: geração mensal de pagamentos, aplicação de regras de elegibilidade e desconto, validações cadastrais críticas.
- Inclusão: autenticação de API com JWT/OAuth2 para proteger os endpoints do ciclo.
- Exclusão: UX avançada e integrações bancárias legadas descontinuadas.

## Requisitos EARS

```yaml
REQ-PAY-001:
  pattern: event-driven
  text: "Quando um ciclo mensal de pagamento for gerado, o SIFAP deverá criar registros de pagamento somente para beneficiários com status ACTIVE."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN#L120-L168
  acceptance:
    - "Dado 10 beneficiários ACTIVE e 2 SUSPENDED, quando o ciclo mensal for executado, então 10 registros de pagamento devem ser criados."
    - "Dado um beneficiário com status CANCELLED, quando o ciclo mensal for executado, então nenhum pagamento deve ser criado para esse beneficiário."
  priority: P0
```

```yaml
REQ-PAY-002:
  pattern: state-driven
  text: "Enquanto o beneficiário estiver na região especial 99, o SIFAP deverá marcá-lo como elegível sem aplicar validações padrão de programa/renda/idade, mantendo obrigatórias as validações cadastrais e documentais."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/VALELEG.NSN#L107-L111
  acceptance:
    - "Dado beneficiário com COD-REGIAO=99 e documentação válida, quando a elegibilidade for avaliada, então o resultado deve ser ELEGIVEL sem aplicar critérios de programa/renda/idade."
    - "Dado beneficiário com COD-REGIAO=99 e documentação inválida, quando a elegibilidade for avaliada, então o resultado deve ser NAO_ELEGIVEL."
    - "Dado beneficiário com COD-REGIAO diferente de 99, quando a elegibilidade for avaliada, então as regras padrão devem ser aplicadas."
  priority: P0
```

```yaml
REQ-PAY-003:
  pattern: unwanted
  text: "O SIFAP não deverá permitir que a soma de descontos não judiciais ultrapasse 30% do valor bruto do pagamento."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/CALCDSCT.NSN#L142-L148
  acceptance:
    - "Dado valor bruto de 1000 e descontos não judiciais totalizando 350, quando o cálculo for executado, então o total de descontos não judiciais deve ser limitado a 300."
    - "Dado descontos não judiciais totalizando 250 para valor bruto 1000, quando o cálculo for executado, então o total deve permanecer 250."
  priority: P0
```

```yaml
REQ-PAY-004:
  pattern: event-driven
  text: "Quando houver desconto judicial no pagamento, o SIFAP deverá aplicar o valor judicial sem teto de 30% e somá-lo ao total de descontos para composição do valor líquido."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/CALCDSCT.NSN#L181-L186
  acceptance:
    - "Dado valor bruto de 1000 e desconto judicial de 500, quando o cálculo for executado, então o desconto judicial aplicado deve ser 500."
    - "Dado desconto judicial de 200 e não judicial de 400 em valor bruto 1000, quando o cálculo for executado, então o total final de desconto deve ser 500."
  priority: P0
```

```yaml
REQ-BEN-001:
  pattern: ubiquitous
  text: "O SIFAP deverá aceitar apenas os status A, S, C, I ou D no cadastro de beneficiário."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/VALBENEF.NSN#L164-L169
  acceptance:
    - "Dado status A, quando o cadastro for validado, então a validação deve ser aprovada."
    - "Dado status X, quando o cadastro for validado, então a validação deve falhar com mensagem de domínio."
  priority: P1
```

```yaml
REQ-BEN-002:
  pattern: unwanted
  text: "O SIFAP não deverá aceitar CPF com todos os dígitos iguais, exceto quando o CPF tiver prefixo especial autorizado (`000`, `001`, `002`, `010`, `011`, `099`, `100`, `999`) conforme exceção legada."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/VALDOCS.NSN#L168-L179
  acceptance:
    - "Dado CPF 11111111111, quando a validação for executada, então o CPF deve ser rejeitado."
    - "Dado CPF 00012345678, quando a validação for executada, então o CPF deve seguir fluxo de exceção legada."
    - "Dado CPF 99912345678, quando a validação for executada, então o CPF deve seguir fluxo de exceção legada."
  priority: P1
```

```yaml
REQ-BEN-003:
  pattern: event-driven
  text: "Quando um dependente for incluído, o SIFAP deverá bloquear a inclusão para beneficiários com status C ou D."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/CADDEPEND.NSN#L56-L59
  acceptance:
    - "Dado beneficiário com status C, quando um novo dependente for incluído, então a inclusão deve ser rejeitada."
    - "Dado beneficiário com status A, quando um novo dependente for incluído, então a inclusão deve seguir para validações seguintes."
  priority: P1
```

```yaml
REQ-BEN-004:
  pattern: unwanted
  text: "O SIFAP não deverá permitir mais de 5 dependentes ativos por beneficiário no fluxo operacional padrão."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/CADDEPEND.NSN#L63-L66
  acceptance:
    - "Dado beneficiário com 5 dependentes, quando tentar incluir o 6º dependente, então a operação deve ser bloqueada."
    - "Dado beneficiário com 4 dependentes, quando incluir o 5º dependente, então a operação deve ser permitida."
  priority: P1
```

## Itens Greenfield

```yaml
REQ-SEC-001:
  pattern: ubiquitous
  text: "O SIFAP deverá autenticar chamadas da API por token JWT válido emitido por provedor OAuth2 corporativo."
  source_legacy: "[GREENFIELD] O legado usa sessão de terminal Natural; a API moderna requer autenticação stateless interoperável."
  acceptance:
    - "Dado token JWT válido, quando a API for chamada, então a requisição deve ser autorizada conforme escopo."
    - "Dado ausência de token ou token inválido, quando a API for chamada, então o retorno deve ser 401."
  priority: P0
```

## Rastreabilidade
- Baseado em `01-arqueologia/discovery-report.md` (prioridades do Estágio 2).
- Baseado em `01-arqueologia/business-rules-catalog.md` (BR-001 a BR-015).
- Baseado em `01-arqueologia/mysteries-found.md` (exceções regionais e de desconto).
