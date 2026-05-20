<!-- markdownlint-disable MD013 MD025 MD040 -->

# ADR-004 — Parametrização de constantes financeiras (FATOR-K e descontos)

- **Status:** Proposta
- **Data:** 20/05/2026
- **Decisores:** Par 2 — Software Architect (lead), Enterprise Architect
- **Relacionada:** [REQ-ADM-004](SPECIFICATION.md#req-adm-004--ajuste-do-valor-base-do-programa-pelo-fator-k), REQ-PAY-001, REQ-PAY-003
- **Sucessora de:** —
- **Substitui:** nenhuma

## Contexto

Três constantes com efeito financeiro estão hoje **hardcoded** no protótipo modernizado e/ou no legado:

| Constante | Valor | Origem | Local atual |
|---|---|---|---|
| `FATOR-K` (multiplicador de reajuste) | `0.347215` | [CADPROG.NSN#L86-L88](../01-arqueologia/legado-sifap/natural-programs/CADPROG.NSN) — inserido em ago/2008 sem documento de origem (MYS-003) | ainda **não implementado** no protótipo Java |
| Teto não-judicial | `0.30` (30 %) | [CALCDSCT.NSN](../01-arqueologia/legado-sifap/natural-programs/CALCDSCT.NSN) | hardcoded em [PaymentService.java#L24](../src/main/java/com/sifap/payment/PaymentService.java#L24) |
| Abono natalino | `0.15` (15 %) | [CALCBENF.NSN](../01-arqueologia/legado-sifap/natural-programs/CALCBENF.NSN) | hardcoded em [PaymentService.java#L26](../src/main/java/com/sifap/payment/PaymentService.java#L26) |

REQ-ADM-004 já exige explicitamente que `0.347215` seja **externalizável via configuração de sistema** (chave `FATOR_K_CONSTANTE`) e [.github/copilot-instructions.md](../.github/copilot-instructions.md) define essa classe de constantes como **proibidas em hardcode**: "fator K = 0.347215 é o exemplo cardinal — sempre parametrize". O risco é uniforme: alteração de qualquer dos três valores hoje obriga recompilação, perde rastreabilidade de auditoria e viola controles regulatórios sobre cálculo financeiro de benefícios.

A passagem H2 deixou em aberto: "Onde o FATOR-K ficará configurado e como isso será coberto por testes?" — esta ADR responde.

## Opções consideradas

### Opção 1 · Constantes em código com `static final`

- **Descrição:** manter como está hoje (`private static final BigDecimal NON_JUDICIAL_CAP_RATIO = new BigDecimal("0.30");`).
- **Vantagens:** zero infraestrutura adicional; desempenho ótimo.
- **Desvantagens:** viola REQ-ADM-004 e a regra rígida do projeto contra hardcode financeiro; alteração obriga rebuild + redeploy; não há trilha de quem mudou o valor.

### Opção 2 · `@ConfigurationProperties` Spring Boot (YAML/env)

- **Descrição:** classe `SifapFinancialProperties` com `@ConfigurationProperties(prefix = "sifap.financial")`, lida de [application.yml](../src/main/resources/application.yml) e sobreposta por env vars (`SIFAP_FINANCIAL_FATOR_K`).
- **Vantagens:** padrão Spring; tipado (`BigDecimal`); validável com `@Min`/`@DecimalMin`; profile-aware (`dev`, `prod`); plugado em testes via `@TestPropertySource`.
- **Desvantagens:** mudança em produção exige redeploy (não hot-reload); não há aprovação de quem aplica.

### Opção 3 · Tabela de parâmetros no banco (`system_parameter`)

- **Descrição:** entidade `SystemParameter (key, value, valid_from, valid_to, updated_by)` em PostgreSQL; valores carregados em cache na inicialização e invalidados via evento.
- **Vantagens:** rastreabilidade completa (`updated_by`, histórico); alteração em runtime sem deploy; integra com `audit_event` (REQ-AUD-001) por construção.
- **Desvantagens:** complexidade extra (cache, invalidação); risco de fallback indefinido se cache falha; exige UI de admin para ser útil — fora do escopo MVP.

### Opção 4 · Híbrido: `@ConfigurationProperties` agora + tabela como evolução

- **Descrição:** Opção 2 imediatamente, com schema da Opção 3 deixado em ADR de evolução.
- **Vantagens:** atende REQ-ADM-004 e desbloqueia o Par 3 hoje; evolução incremental sem retrabalho disruptivo.
- **Desvantagens:** parte do valor (rastreabilidade fina) só vem na fase 2.

## Decisão

**Adotamos a Opção 4 (Híbrido).**

Para a release MVP do SIFAP 2.0:

1. Criar a classe `br.gov.sifap.admin.application.SifapFinancialProperties` com `@ConfigurationProperties(prefix = "sifap.financial")` expondo:
    - `factorKConstant: BigDecimal` (default `0.347215`)
    - `nonJudicialCapRatio: BigDecimal` (default `0.30`)
    - `christmasBonusRatio: BigDecimal` (default `0.15`)
2. Validações Bean Validation: `@DecimalMin("0.0")`, `@DecimalMax("1.0")` para os ratios; `@DecimalMin("0.0")` para `factorKConstant`.
3. Valores carregados de `application.yml` e sobrescritos por env vars (`SIFAP_FINANCIAL_FACTOR_K_CONSTANT`, etc.).
4. **Proibido** referenciar essas constantes por nome literal fora do bean. `PaymentService` e `SocialProgramService` recebem o bean via construtor.
5. Logar (sem PII) o valor efetivo na inicialização: `factorKConstant=0.347215, nonJudicialCapRatio=0.30, christmasBonusRatio=0.15`.
6. Cobertura de teste obrigatória:
    - teste unitário com `@TestPropertySource` sobrescrevendo cada propriedade;
    - teste de regressão fixando o valor legado para garantir paridade.
7. Registrar tarefa no roadmap (não no MVP) para evolução em **ADR-005** propondo `system_parameter` com auditoria via `audit_event`.

## Justificativa

- **REQ-ADM-004 explicitamente cita externalização via configuração**, não exige tabela.
- **Opção 1** é vetada por política do projeto (`copilot-instructions.md` § "Regras rígidas").
- **Opção 3** isolada é over-engineering para a janela do workshop e cria fronteira nova entre `admin` e qualquer módulo consumidor antes de termos UI/processo de aprovação.
- **Opção 4** entrega o valor de segurança imediato (sem rebuild para reajuste), mantém custo cognitivo baixo e abre caminho linear para evolução governada.

## Consequências

### Positivas

- Elimina o último hardcode financeiro identificado pelo codemap (smell 6 e 7 em [docs/codemap-payment.md](../docs/codemap-payment.md)).
- Destrava a passagem H2 (resposta à pergunta 4 do [H2-HANDOFF-PAR3.md](H2-HANDOFF-PAR3.md)).
- Permite ao QA Engineer (Par 4) escrever testes parametrizados para cenários de reajuste sem precisar mexer em código.
- Compatível com a constituição existente do projeto e com o ADR-001.

### Negativas

- Mudança de valor em produção exige redeploy. **Mitigação:** ADR-005 futuro com tabela e cache invalidável.
- Risco de developer adicionar nova constante financeira fora do bean. **Mitigação:** lint/check no PR (regex `new BigDecimal\("0\.\d+"\)` em pacote de domínio financeiro vira aviso obrigatório).

### Riscos

- **R1 — Discrepância numérica entre legado e moderno por ordem de operações.** Ainda que externalize o `0.347215`, a fórmula `(1 + FATOR_REAJ * 0.347215)` deve ser aplicada na mesma ordem do legado (CADPROG.NSN#L87-L88). Plano: teste de regressão usando vetor de inputs do dump legado.
- **R2 — Fallback silencioso quando propriedade ausente.** Configurar `failOnMissing=true` (sem default mágico em produção); `application-prod.yml` declara explicitamente os 3 valores.

## Relação com a spec

- Resolve operacionalmente o critério de aceitação 4 do REQ-ADM-004 ("o valor 0.347215 deve ser externalizável via configuração de sistema").
- Reforça regras 4 e 5 do [ADR-001](ADR-001-monolito-modular.md): `payment` consome a configuração via porta de aplicação fornecida por `admin`, sem importar classes de infraestrutura.
- Não altera [ADR-002](ADR-002-estrategia-integracao.md) nem [ADR-003](ADR-003-auditoria-imutavel.md).

## Plano de adoção

| Passo | Owner | Quando |
|---|---|---|
| Criar `SifapFinancialProperties` + `@EnableConfigurationProperties` | Par 3 (Dev) | Sprint 1 do Estágio 3 |
| Refatorar `PaymentService` para injetar o bean | Par 3 | Sprint 1 |
| Implementar `REQ-ADM-004` em `SocialProgramService` usando `factorKConstant` | Par 3 | Sprint 1 |
| Testes parametrizados de regressão (vetor legado) | Par 4 (QA) | Sprint 1 |
| Logar valores efetivos na inicialização | Par 3 | Sprint 1 |
| ADR-005 (tabela `system_parameter` + auditoria) | Par 2 + DBA | Pós-MVP |
