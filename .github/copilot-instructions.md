# Instruções do GitHub Copilot — Time Rosa-02 · SIFAP 2.0

Este repositório moderniza o legado **SIFAP** (Sistema de Fiscalização e Administração de Pagamentos) de Natural/Adabas para uma stack Java + Next.js + Azure. Siga rigorosamente estas instruções em todas as sugestões.

## Estado atual do projeto

- **Time:** Rosa-02 · 5 pessoas · 10 personas · 5 pares
- **Estágio em andamento:** 2 (Spec Moderna) — Passagem #2 (H2) em curso
- **Branch ativa:** `develop`
- **Spec ativa:** `specs/002-geracao-ciclo-pagamento/`
- **Aprovação PO:** ✅ concedida em 20/05/2026 (sem condicionantes pendentes)
- **REQ-ADM-004 (fator K):** ✅ formalizado — bloqueio do Estágio 3 removido
- **Próximo gate:** validação técnica do Par 2 (Arquitetura) e fit-check do Par 3 (Implementação)

## Contexto de domínio (não invente)

- **SIFAP legado:** 15 programas Natural (`.NSN`) + 4 DDMs Adabas, ~29 anos de regras de negócio
- **Volume:** 2,3 milhões de beneficiários · ciclo mensal de pagamento é **sagrado**
- **Constante mágica do legado:** `0.347215` no cálculo do fator K (CADPROG.NSN#L87-L88) — **deve virar parâmetro configurável**, nunca hardcode
- **Mascaramento de CPF:** sempre `XXX.XXX.NNN-NN` em logs, APIs e relatórios

## Stack-alvo (use exatamente estas versões)

| Camada      | Tecnologia                                                     |
| ----------- | -------------------------------------------------------------- |
| Backend     | Java 21 + Spring Boot 3.3 + JPA/Hibernate + Flyway             |
| Banco       | PostgreSQL 16                                                  |
| Frontend    | Next.js 15 (App Router) + TypeScript 5 strict + Tailwind + shadcn/ui |
| Containers  | Docker + Docker Compose                                        |
| IaC         | Terraform com provider AzureRM `~> 3.x`                        |
| CI/CD       | GitHub Actions                                                 |
| Testes      | JUnit 5 + Testcontainers (back) · Vitest + Testing Library (front) |

Não sugira frameworks fora desta lista. Não proponha NoSQL, MongoDB, Express, Quarkus, Micronaut, Vue, Angular puro, Pulumi, Bicep, CDK ou outras alternativas.

## Toolchain aprovada (somente estas)

- **VS Code** + **GitHub Copilot** (modos Ask, Plan, Agent) + **Copilot CLI** (opcional)
- **GitHub Spec-Kit** (`Specify CLI` + comandos `/speckit.*`) — único framework SDD permitido
- **GitHub** (Issues, PRs, Actions, Projects) — fonte da verdade
- **Docker Compose** local · **Terraform** para Azure

❌ Proibido: Cursor, Windsurf, Codex, Cline, Continue, Aider, Codeium, Tabnine, IntelliJ, Eclipse, Kiro, pipelines SDD customizados ou UIs web de chat.

## Spec-Driven Development — regras invioláveis

1. Todo requisito usa **notação EARS** (6 padrões: ubiquitous, event-driven, state-driven, optional, unwanted, complex)
2. Todo requisito tem **REQ-ID** no formato `REQ-<DOMAIN>-NNN` (ex.: `REQ-PAY-001`, `REQ-ADM-004`)
3. **Todo requisito carrega `source_legacy:`** apontando para:
   - `01-arqueologia/legado-sifap/natural-programs/<programa>.NSN#L<linha>-L<linha>`
   - `01-arqueologia/legado-sifap/adabas-ddms/<arquivo>.ddm#L<linha>`
   - ou `[GREENFIELD]` com justificativa de 1 linha
4. O job de CI `legacy-traceability` **rejeita PRs** sem `source_legacy:` válido
5. Todo requisito tem critérios de aceitação no formato Given/When/Then
6. Testes referenciam o REQ-ID em comentários inline (ex.: `// covers REQ-PAY-001`)
7. Branch por spec: `spec/<NNN>-<feature>` · merge `spec/*` → `develop` → `stage` → `main`

## Convenções de código

### Java
- Records para DTOs, sealed interfaces para uniões discriminadas, pattern matching, virtual threads
- `Optional` em retornos públicos — **nunca retorne `null`**
- `@Transactional` somente em services (nunca em repositories)
- Validação na controller com `@Valid` + Bean Validation
- Nomes de classes e comentários em **inglês**
- Mascarar dados sensíveis (CPF, valor de benefício) em todo log
- `BigDecimal` com `RoundingMode.DOWN` para preservar truncamento legado (REQ-PAY-004)

### TypeScript / Next.js
- `strict: true` no `tsconfig.json` — sem exceções
- Server actions para mutations · client components nunca acessam secrets
- `async/await` (nunca cadeias `.then()`)
- Apenas **named exports** (sem `export default`)

### REST
- Path: `/api/v1/{resource}` · verbos HTTP corretos · status codes apropriados (`201`, `204`, `409`, `422`)
- Toda rota tem annotation OpenAPI/Swagger
- Erros no formato Problem Details (RFC 7807)

### Terraform
- `tags` obrigatória em todo recurso: `project`, `environment`, `owner`
- Secrets só via `azurerm_key_vault_secret` (nunca em `locals` ou `variables`)
- 1 módulo por área Azure (networking, compute, database, monitoring)
- `terraform fmt` + `terraform validate` antes de qualquer commit

### SQL / Migrations
- Flyway com nomeação `V<seq>__<descricao_snake_case>.sql`
- Migrations idempotentes e nunca editadas após merge — sempre uma nova migration
- `audit_event` é append-only — `UPDATE`/`DELETE` retornam `403`

## Segurança (OWASP Top 10)

- Validação de entrada em toda fronteira
- Zero hardcode de secrets, API keys, credenciais ou constantes mágicas com efeito financeiro
- SQL apenas via JPA/JPQL — nunca concatenação de string
- CORS explícito (sem `*` em produção)
- Auth via OAuth2/JWT (Spring Security)
- Recursos Azure usam **Managed Identity** para auth serviço-a-serviço

## Estrutura de personas (Par 1 · Visão é o autor deste contexto)

| Par | Personas                                  | Lidera                          |
| --- | ----------------------------------------- | ------------------------------- |
| 1   | Product Owner + Requirements Engineer     | Descoberta + Especificação      |
| 2   | Enterprise Architect + Software Architect | Especificação + Design          |
| 3   | Technical Lead + Developer                | Implementação + Evolução        |
| 4   | DBA + QA Engineer                         | Implementação (dados + testes)  |
| 5   | DevOps Engineer + Tech Writer             | Transversal + Evolução          |

Agentes instalados em `.github/agents/`: `archaeologist`, `architect`, `builder`, `evolution`, `product-owner`, `requirements-engineer`, `tech-lead`, `implementer`, `dba`, `qa-engineer`.

Quando o usuário invocar um agente (`@archaeologist`, `@architect`, etc.), assuma o framing daquele agente. Quando rodar prompts (`/spec`, `/ears-convert`, `/audit-context`, etc.), siga o template do prompt em `.github/prompts/`.

## Instruções específicas por área

Os arquivos em `.github/instructions/` são aplicados automaticamente conforme o campo `applyTo`:

- `natural-adabas.instructions.md` — leitura de `.NSN`, `.cpy`, `.ddm`
- `requirements.instructions.md` — todo o repositório (regras de EARS)
- `modular-monolith.instructions.md` — código Java sob `src/main/java/**`
- `frontend-spec.instructions.md` — código TS/TSX sob `app/**` e `components/**`
- `database.instructions.md` — todo o repositório (migrations + auditoria)
- `tests.instructions.md` — todo o repositório (TDD + cobertura)

## Regras rígidas — nunca faça

- ❌ Sugerir código sem antes consultar `prototype/` (symlink criado por `11-scripts/setup.sh`)
- ❌ Escrever EARS sem `source_legacy:` (CI rejeita o PR)
- ❌ Adicionar dependência sem ADR justificando
- ❌ Escrever testes depois do código — sempre TDD ou test-first
- ❌ Expor secrets em commits, logs, descrições de PR ou client components
- ❌ Hardcode de constantes financeiras (fator K = `0.347215` é o exemplo cardinal — sempre parametrize)
- ❌ Merge em `main` sem revisão entre pares
- ❌ Pular conversas guiadas de passagem (H1, H2, H3) — ver `00-TEAM-FLOW.md`
- ❌ Usar `null` em retornos públicos Java · `any` em TypeScript · `SELECT *` em SQL
- ❌ Reescrever programa Natural linha por linha — modernize o **comportamento**, não a sintaxe

## Modos do Copilot — guia rápido

| Modo  | Quando usar                                        | Exemplo                                                  |
| ----- | -------------------------------------------------- | -------------------------------------------------------- |
| Ask   | Explorar, debater trade-offs, entender legado      | "Explique `BATCHPGT.NSN` linha por linha"                |
| Plan  | Mudanças multi-arquivo antes de executar           | "Planeje o bounded context `payment` com 3 camadas"      |
| Agent | Delegar feature completa via Issue → PR            | "Implemente REQ-PAY-005 com Testcontainers e auditoria"  |

## Referências internas (uso frequente)

- [`00-TEAM-FLOW.md`](../00-TEAM-FLOW.md) — linha do tempo, passagens, escalonamento
- [`01-arqueologia/LEGACY-EXPLORATION-CHECKLIST.md`](../01-arqueologia/LEGACY-EXPLORATION-CHECKLIST.md) — HARD GATE pré-Estágio 2
- [`01-arqueologia/business-rules-catalog.md`](../01-arqueologia/business-rules-catalog.md) — 15 regras (BR-001..BR-015)
- [`01-arqueologia/mysteries-found.md`](../01-arqueologia/mysteries-found.md) — 10 mistérios + 3 easter eggs
- [`02-spec-moderna/SPECIFICATION.md`](../02-spec-moderna/SPECIFICATION.md) — spec mestre (v0.2.0)
- [`02-spec-moderna/scope-decisions.md`](../02-spec-moderna/scope-decisions.md) — escopo aprovado pelo PO
- [`specs/002-geracao-ciclo-pagamento/`](../specs/002-geracao-ciclo-pagamento/) — spec ativa
- [`09-cheat-sheets/`](../09-cheat-sheets/) — Copilot, Spec-Kit, roteamento de modelo

## Default em caso de dúvida

> Quando em dúvida entre paridade legada e modernização: preserve o **comportamento** observável do legado, mas **modernize a implementação**. Toda divergência intencional precisa de REQ-ID + ADR.
