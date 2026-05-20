<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Spec 002 — Geração de Ciclo de Pagamento

## Contexto
Esta spec transforma descobertas do Estágio 1 em requisitos EARS testáveis para o fluxo de geração mensal de pagamentos do SIFAP. O foco inicial cobre cálculo, elegibilidade e validações críticas que impactam pagamento, conformidade e conciliação.

## Escopo
- Inclusão: geração mensal de pagamentos, aplicação de regras de elegibilidade e desconto, validações cadastrais críticas.
- Inclusão: cálculo do fator K (correção especial do programa social) usado como base do valor de pagamento — REQ-ADM-004 é pré-condição financeira do ciclo.
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
  text: "O SIFAP não deverá permitir que o total acumulado de descontos (incluindo judiciais previamente somados) exceda 30% do VLR-BRUTO no momento em que um desconto não judicial for processado; quando o teto for atingido, o total deve ser truncado para 30% × VLR-BRUTO."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/CALCDSCT.NSN#L102,#L160-L165
  business_rule: BR-006
  formula:
    teto: "VLR-MAX-DSCT = VLR-BRUTO × 0.30  (cap calculado sobre o BRUTO INTEGRAL, não sobre bruto-judicial)"
    aplicacao: "truncamento dispara apenas quando TIPO-DSCT ≠ 'J' e VLR-TOTAL-DSCT (acumulado) > VLR-MAX-DSCT"
  acceptance:
    - "Dado VLR-BRUTO=1000 e descontos não judiciais somando 350, quando o cálculo for executado, então VLR-TOTAL-DSCT deve ser truncado para 300 (= 1000 × 0.30)."
    - "Dado VLR-BRUTO=1000 e descontos não judiciais somando 250, quando o cálculo for executado, então VLR-TOTAL-DSCT deve permanecer 250."
    - "Dado VLR-BRUTO=1000 com desconto I=200 processado antes de J=500, quando o cálculo for executado, então VLR-TOTAL-DSCT final deve ser 700 (cap não disparou em I=200; J entra livre depois)."
    - "Dado VLR-BRUTO=1000 com desconto J=500 processado antes de I=100, quando o cálculo for executado, então VLR-TOTAL-DSCT final deve ser 300 (paridade legada: o cap aplicado em I trunca também o judicial já acumulado)."
  priority: P0
  note: |
    Fórmula extraída de CALCDSCT.NSN#L102 (cap = bruto × 0.30) e #L160-L165 (truncamento
    aplicado apenas quando o item corrente é não judicial, sobre acumulado total).
    O 4º critério documenta um efeito ordem-dependente do legado: a ordem de processamento
    dos descontos no PE-GROUP de BENEFICIARIO altera o resultado final.
    Decisão de modernização (REQ-PAY-007 a criar pelo PO/RE): preservar paridade no v1.0
    e propor refatorável para v1.1 após alinhamento jurídico (judicial deveria ser
    sempre excluído do cômputo do cap, mas isso muda comportamento histórico).
```

```yaml
REQ-PAY-004:
  pattern: event-driven
  text: "Quando um desconto do tipo judicial (TIPO-DSCT='J') for processado, o SIFAP deverá somá-lo a VLR-TOTAL-DSCT sem disparar a verificação do teto de 30% naquele item; o teto só é verificado em itens com TIPO-DSCT ≠ 'J'."
  source_legacy: 01-arqueologia/legado-sifap/natural-programs/CALCDSCT.NSN#L160-L165
  business_rule: BR-006
  mystery_ref: MYS-006
  acceptance:
    - "Dado VLR-BRUTO=1000 e único desconto J=500, quando o cálculo for executado, então VLR-TOTAL-DSCT deve ser 500 (sem aplicação de teto)."
    - "Dado VLR-BRUTO=1000 com J=200 + I=400 processados em sequência (J primeiro), quando o cálculo for executado, então VLR-TOTAL-DSCT final deve ser 300 (J=200 acumula livre; I=400 → acumulado=600 > 300 → trunca a 300; comportamento legado)."
    - "Dado VLR-BRUTO=1000 com I=400 + J=200 processados em sequência (I primeiro), quando o cálculo for executado, então VLR-TOTAL-DSCT final deve ser 500 (I=400 → acumulado=400 > 300 → trunca a 300; J=200 entra livre depois → 500)."
  priority: P0
  note: "Ordem de iteração do PE-GROUP em CALCDSCT.NSN#L107 (FOR #IDX = 1 TO C*DESCONTOS) preserva ordem de inserção no Adabas. No moderno, garantir ordenação estável por (DT-INICIO-DSCT, ordem de inserção) para paridade."
```

```yaml
REQ-ADM-004:
  pattern: event-driven
  text: "Quando um programa social for cadastrado ou tiver seu FATOR-REAJ atualizado, o SIFAP deverá calcular o fator de correção especial (fator K) pela fórmula `fator_k = 1.00 + (FATOR-REAJ × CONSTANTE_K)` e persistir o VLR-BASE-AJUSTADO = VLR-BASE × fator_k, usado como base de cálculo de todo pagamento subsequente."
  source_legacy:
    - 01-arqueologia/legado-sifap/natural-programs/CADPROG.NSN#L86-L92
    - 01-arqueologia/legado-sifap/adabas-ddms/PROGRAMA-SOCIAL.ddm#L39
  business_rule: BR-006
  mystery_ref: MYS-003
  acceptance:
    - "Dada CONSTANTE_K=0.347215 (valor legado) e FATOR-REAJ=0.10, quando o programa for cadastrado, então fator_k deve ser 1.0347215 e VLR-BASE-AJUSTADO = VLR-BASE × 1.0347215 truncado a 2 casas (RoundingMode.DOWN)."
    - "Dado FATOR-REAJ=0.00 ou nulo, quando o programa for cadastrado, então fator_k deve ser 1.00 e VLR-BASE-AJUSTADO = VLR-BASE."
    - "Dado FATOR-REAJ atualizado em programa existente, quando o cadastro for salvo, então fator_k e VLR-BASE-AJUSTADO devem ser recalculados e um audit_event do tipo PROGRAM_K_UPDATED deve ser registrado com valor anterior e novo."
    - "Dada CONSTANTE_K configurada via tabela admin.factor_constants (não hardcoded), quando o serviço iniciar, então o valor deve ser lido da configuração e o range deve ser validado entre 0.10 e 0.99."
    - "Dado pagamento gerado pelo ciclo (REQ-PAY-001), quando o valor base for calculado, então deve usar VLR-BASE-AJUSTADO do programa associado (não o VLR-BASE bruto)."
  priority: P0
  risk: CRÍTICO
  note: "Constante mágica 0.347215 documentada em CADPROG.NSN#L87 sem origem documental (MYS-003). PO determinou parametrização com paridade histórica via Flyway. Pré-condição financeira para REQ-PAY-001: sem fator K aplicado, todos os pagamentos do ciclo divergem do legado."
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
- Baseado em `01-arqueologia/business-rules-catalog.md` (BR-001 a BR-015, com ênfase em BR-006 para fator K).
- Baseado em `01-arqueologia/mysteries-found.md` (MYS-003 fator K, MYS-006 desconto judicial, MYS-008 região 99).
- Referência ao master `02-spec-moderna/SPECIFICATION.md` v0.2.0 (sign-off PO 20/05/2026).