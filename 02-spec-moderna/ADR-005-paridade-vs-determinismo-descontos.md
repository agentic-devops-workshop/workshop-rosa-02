<!-- markdownlint-disable MD013 MD025 MD040 -->

# ADR-005 — Determinismo do cálculo de descontos vs. paridade legada

- **Status:** Proposta
- **Data:** 20/05/2026
- **Decisores:** Par 2 — Software Architect (lead), Enterprise Architect
- **Requer aprovação adicional de:** Par 1 — Product Owner (impacto financeiro/regulatório)
- **Relacionada:** REQ-PAY-001 (cap não-judicial), REQ-PAY-002 (judicial sem teto), [ADR-004](ADR-004-parametrizacao-constantes-financeiras.md)
- **Origem:** `/speckit.analyze` de [specs/002-geracao-ciclo-pagamento](../specs/002-geracao-ciclo-pagamento/) — bloqueador B3 promovido a divergência funcional após leitura de [CALCDSCT.NSN#L100-L170](../01-arqueologia/legado-sifap/natural-programs/CALCDSCT.NSN)

## Contexto

A análise do legado revelou que [CALCDSCT.NSN](../01-arqueologia/legado-sifap/natural-programs/CALCDSCT.NSN) calcula o teto de 30 % de descontos com algoritmo **dependente da ordem de iteração** do `PE group` Adabas:

```natural
COMPUTE #VLR-MAX-DSCT = #VLR-BRUTO * 0.30          ← cap = 30% do BRUTO
MOVE 0 TO #VLR-TOTAL-DSCT

FOR #IDX = 1 TO C*DESCONTOS                         ← itera na ordem cadastrada
    ... ADD #VLR-DSCT-ITEM TO #VLR-TOTAL-DSCT       ← acumula sempre

    IF #TIPO-DSCT NE 'J'                            ← clamp só quando NÃO é judicial
      IF #VLR-TOTAL-DSCT > #VLR-MAX-DSCT
        MOVE #VLR-MAX-DSCT TO #VLR-TOTAL-DSCT       ← clamp do TOTAL ACUMULADO
      END-IF
    END-IF
END-FOR
```

### Consequência: o resultado depende da ordem de cadastro do desconto

Mesmo com beneficiário, valores e tipos idênticos, o total muda conforme a ordem do PE group:

| Ordem dos itens | bruto | judicial | não-judicial | total legado |
|---|---:|---:|---:|---:|
| **A** — não-judicial primeiro | 1000 | 500 | 400 | **800** (clamp em iter 1: 400 → 300; depois soma 500) |
| **B** — judicial primeiro | 1000 | 500 | 400 | **300** (soma 500; depois iter 2 acumula 400 → total 900 → clamp para 300, "comendo" 500 do judicial) |

A diferença pode chegar a **R$ 500 entre dois beneficiários idênticos** apenas porque um cadastrou os descontos em ordem diferente. Não há documento de negócio justificando esse comportamento — trata-se de bug histórico do legado, não regra deliberada.

### Comportamento atual do protótipo SIFAP 2.0

[PaymentService.applyDiscounts](../src/main/java/com/sifap/payment/PaymentService.java#L102-L120) implementa hoje:

```java
nonJudicialApplied = nonJudicialRequested.min(cap);   // clamp só do subtotal NJ
totalDiscount = nonJudicialApplied + judicialTotal;   // judicial integral
```

**Order-independent**, sempre produz o resultado da ordem A (800 no exemplo). Não há tratamento de divergência registrado, e o teste [PaymentServiceTest](../src/test/java/com/sifap/payment/PaymentServiceTest.java#L93-L113) congela apenas o caminho A.

### Por que isso vira ADR

Sem decisão explícita, qualquer escolha viola o default do projeto: "preserve o **comportamento** observável do legado, mas modernize a implementação. Toda divergência intencional precisa de REQ-ID + ADR" ([copilot-instructions.md](../.github/copilot-instructions.md) § "Default em caso de dúvida").

## Opções consideradas

### Opção 1 · Paridade legada estrita

- **Descrição:** reproduzir o loop com clamp acumulado, persistindo a ordem de cadastro original do PE group em coluna dedicada de `payment_discount`.
- **Vantagens:** zero divergência numérica em migração big-bang; aceita por auditoria que exige paridade total.
- **Desvantagens:** codifica um bug; resultado pode descontar mais do que devido para parte da população; cria entropia em testes (matriz de permutações de ordem); torna `applyDiscounts` não-comutativo.
- **Risco regulatório:** **alto** — uma denúncia de beneficiário descontado a mais por mero acaso de ordem é defensável judicialmente.

### Opção 2 · Determinismo corrigido (subtotal-by-type)

- **Descrição:** clamp aplicado **apenas** sobre o subtotal não-judicial (`min(sum_NJ, cap)`); judicial integral somado em seguida. **É o que o protótipo já faz.**
- **Vantagens:** order-independent; previsível; alinha com pareceres de proteção ao beneficiário (cap do legado nunca pretendeu reduzir judicial); simples de testar; comutativo.
- **Desvantagens:** divergência numérica intencional vs. legado para registros cadastrados em ordem B — exige plano de comunicação e relatório de reconciliação na migração.

### Opção 3 · Política do mínimo (defensiva)

- **Descrição:** calcular ambos (paridade e determinismo) e gravar o **menor**: `total = min(opcao1, opcao2)`. Garante que ninguém receba menos do que o legado já lhe descontaria.
- **Vantagens:** zero risco de "modernizar penalizando" — é monotônico em favor do beneficiário.
- **Desvantagens:** preserva o pior caso do legado para parte da população; mantém comportamento order-dependent dentro do `min`; mais complexo de explicar e auditar; não soluciona a injustiça entre beneficiários da ordem A vs. ordem B.

## Decisão

**Adotamos a Opção 2 (Determinismo corrigido).**

Operacionalização:

1. **Novo requisito** `REQ-PAY-006` em [SPECIFICATION.md](SPECIFICATION.md) (a inserir pelo Par 1 — RE), explicitando a regra determinística e marcando-a como **divergência intencional do legado** com referência a esta ADR. Texto base proposto:

    ```yaml
    REQ-PAY-006:
      pattern: ubiquitous
      text: "O SIFAP deverá aplicar o teto de 30% do valor bruto exclusivamente
             sobre o subtotal de descontos não-judiciais, somando-o
             integralmente aos descontos judiciais para compor o total."
      source_legacy: 01-arqueologia/legado-sifap/natural-programs/CALCDSCT.NSN#L100-L170
      divergence: "DIVERGÊNCIA INTENCIONAL DO LEGADO. O legado aplica o cap sobre
                   #VLR-TOTAL-DSCT acumulado, com efeito que depende da ordem do
                   PE group. SIFAP 2.0 elimina a dependência aplicando o cap
                   apenas sobre o subtotal não-judicial. Ver ADR-005."
      acceptance:
        - "Dado bruto 1000, judicial 500 e não-judicial 400 em qualquer ordem,
           total deve ser 800 (= 500 + min(400, 300))."
        - "Resultado deve ser idêntico independentemente da ordem dos descontos
           informados ao serviço."
      priority: P0
      risk: ALTO
      related_adr: ADR-005-paridade-vs-determinismo-descontos
    ```

2. **Constantes parametrizadas:** o cap de 30 % continua governado por [ADR-004](ADR-004-parametrizacao-constantes-financeiras.md) (`sifap.financial.non-judicial-cap-ratio`). Esta ADR não muda a fonte do número, só a álgebra.

3. **Teste de regressão obrigatório (Par 4 · QA):** cobrir as duas ordens (A e B) e mais um caso com 3 itens NJ + 1 J em ordem alternada. Em todos, o resultado tem de bater com o computado por `judicial + min(sum_NJ, cap)`.

4. **Reconciliação na migração:** durante a fase de carga inicial (out of scope desta spec, registrar como dívida), produzir relatório `discount-divergence-report.csv` listando todo beneficiário cujo total moderno difere do total legado, com:
    - `cpf` (mascarado), `cycle`, `legacyTotal`, `modernTotal`, `delta`, `legacyOrderHash`.
    - Operação financeira (Par 1) decide caso a caso se há ressarcimento devido.

5. **Bloqueio de regressão:** ArchUnit / lint do projeto adiciona regra que rejeita qualquer commit em `payment` que reintroduza acumulação tipo-agnóstica de descontos antes do clamp.

## Justificativa

- A Opção 1 reproduz comportamento que **não tem origem em regra de negócio escrita** — é efeito colateral da estrutura física do PE group Adabas. Reproduzir bug por princípio de paridade contradiz o próprio espírito da modernização declarado nas instruções do projeto.
- A Opção 3 mantém o pior caso do legado para subset arbitrário da população. É defensivo demais — protege contra cenários onde o legado já estava errado.
- A Opção 2 é a única que entrega **previsibilidade**, requisito implícito de qualquer sistema financeiro auditável: dois beneficiários com mesmos valores devem receber mesmo resultado.

## Consequências

### Positivas

- Resultado de descontos torna-se função pura dos inputs (comutativo, idempotente).
- Elimina classe inteira de bugs de ordenação na migração da estrutura PE group → tabela `payment_discount` relacional.
- Permite simplificar contratos: API de descontos pode aceitar `Set<DiscountRequest>` em vez de `List`.
- Habilita testes baseados em propriedade (property-based) com permutações arbitrárias.
- Beneficiários da "ordem B" no legado deixam de ser penalizados; potencial impacto positivo em relatórios de proteção ao consumidor.

### Negativas

- **Divergência numérica intencional vs. legado**, com possível impacto financeiro residual a ressarcir. Mitigação: relatório de reconciliação obrigatório na migração.
- Não-comutatividade do legado é **histórico oficial** dos cálculos passados — qualquer recálculo retroativo passará a divergir. Mitigação: *carve-out* documentado para histórico (recalcular nunca; manter snapshot legado em `payment_history_legacy`).
- Auditoria externa pode questionar por que SIFAP 2.0 produz números diferentes do SIFAP legado para o mesmo input. Mitigação: ADR-005 como peça primária de defesa, anexa ao manual de migração.

### Riscos

- **R1 — PO recusa a divergência por receio regulatório.** Plano B: cair para Opção 3 (mínimo entre opções) preservando o requisito de relatório de reconciliação.
- **R2 — Auditor externo exige paridade total.** Mitigação: parecer jurídico antecipado sobre a natureza não-deliberada do bug legado; carta de cobertura de migração assinada pelo gestor do programa.
- **R3 — Caso especial de descontos descobertos depois.** Manter ADR aberta a amendment se aparecer tipo de desconto novo (ex.: judicial parcial) que reabra a discussão.

## Alternativas rejeitadas

- **Manter o status quo do código sem ADR.** Recusada — viola explicitamente a regra "toda divergência intencional precisa de REQ-ID + ADR".
- **Tornar o cap configurável por ordem ("legado" vs "moderno") via feature flag.** Recusada — multiplica caminhos de teste e adia decisão; sistema financeiro não pode operar com dois algoritmos de cálculo simultâneos sem causa.

## Relação com a spec

- Cria a necessidade do `REQ-PAY-006` em [SPECIFICATION.md](SPECIFICATION.md).
- Atende ao bloqueador **B3** detectado em `/speckit.analyze` da [spec 002](../specs/002-geracao-ciclo-pagamento/spec.md).
- Não altera ADR-001 (modular monolith), ADR-002 (integração), ADR-003 (auditoria) ou ADR-004 (parametrização).
- Diferimento explícito: o algoritmo de **migração de histórico legado** (sem recálculo) entra em ADR de migração — não escopo desta.

## Plano de adoção

| Passo | Owner | Quando |
|---|---|---|
| Aprovar ADR-005 (status → Aceita) | PO + EA | Antes do encerramento do Estágio 2 |
| Criar `REQ-PAY-006` em SPECIFICATION.md citando esta ADR | Par 1 (RE) | Mesmo dia |
| Adicionar `REQ-PAY-006` na cobertura da [spec 002](../specs/002-geracao-ciclo-pagamento/spec.md) | Par 1 (RE) | Mesmo dia |
| Property-based test em `applyDiscounts` (permutações) | Par 4 (QA) | Sprint 1 do Estágio 3 |
| Regra ArchUnit anti-regressão | Par 3 (TL) | Sprint 1 |
| Definir formato do `discount-divergence-report.csv` | Par 4 (DBA) + Par 1 | Pós-MVP |
| Carta de migração + parecer regulatório | Par 1 (PO) + Par 5 (TW) | Antes da migração de produção |
