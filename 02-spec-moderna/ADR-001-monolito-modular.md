# ADR-001 — Monolito Modular como arquitetura-alvo

- **Status:** Aceita
- **Data:** 20/05/2026
- **Decisores:** Par 2 — Enterprise Architect + Software Architect

## Contexto

O SIFAP legado concentra regras de negócio críticas em 15 programas Natural e 4 DDMs,
com acoplamento predominante por dados compartilhados. A modernização precisa preservar
fronteiras de domínio sem introduzir complexidade operacional incompatível com o workshop.

As evidências da arqueologia apontam quatro bounded contexts claros:

- `beneficiary`
- `payment`
- `admin`
- `audit`

O fluxo mensal de pagamento continua sendo o centro do sistema e exige consistência
entre cálculo, remessa, conciliação e auditoria.

## Decisão

Adotar um monolito modular em Java 21 + Spring Boot 3.3, com fronteiras explícitas por bounded context.

Estrutura lógica alvo:

```text
br/gov/sifap/
  beneficiary/
    domain/
    application/
    infrastructure/
  payment/
    domain/
    application/
    infrastructure/
  admin/
    domain/
    application/
    infrastructure/
  audit/
    domain/
    application/
    infrastructure/
```

Regras obrigatórias:

1. Cada módulo controla seus próprios agregados e contratos.
2. Nenhum módulo acessa diretamente a infraestrutura de outro módulo.
3. Integração entre módulos ocorre por portas, serviços de aplicação ou eventos internos.
4. O banco pode ser compartilhado, mas com separação lógica por schema e ownership por contexto.
5. As fronteiras serão validadas em revisão técnica e, quando possível, por testes arquiteturais.

## Consequências

### Positivas

- Permite decompor o legado em domínios reconhecíveis pelo time.
- Reduz custo operacional comparado a microsserviços.
- Mantém um único deployable, compatível com a janela do workshop.
- Facilita o handoff H2, porque Par 3 implementa por módulo e prioridade.

### Negativas

- Exige disciplina para não cair em pacote por camada genérica.
- O banco compartilhado ainda pode induzir acoplamento se os limites forem ignorados.
- O módulo `audit` pode virar atalho transversal se não houver governança.

## Alternativas consideradas

### Microsserviços desde o início

Rejeitada porque adiciona custo de operação, contratos distribuídos, observabilidade e integração entre serviços sem benefício proporcional para um time pequeno e um prazo de um dia.

### Monolito tradicional sem módulos explícitos

Rejeitada porque reproduz o problema estrutural do legado: fronteiras difusas, baixa rastreabilidade de domínio e alto risco de dependências acidentais.

## Relação com a spec

- Sustenta os 4 bounded contexts definidos em [SPECIFICATION.md](SPECIFICATION.md).
- Dá base para [C4-DIAGRAMS.md](C4-DIAGRAMS.md).
- Direciona o Par 3 a implementar por contexto e não por camada transversal.