# SIFAP 2.0 — Guia da Aplicação

> README operacional da aplicação modernizada (backend Java 21 + frontend Next.js 15).
> Para o kit do workshop (jornada didática), veja [`README.md`](README.md).

**Time:** Rosa-02 · **Branch:** `develop` · **Tests:** 32/32 ✅

---

## Stack

| Camada     | Tecnologia                                           |
|------------|------------------------------------------------------|
| Backend    | Java 21 · Spring Boot 3.3 · JPA · Flyway · springdoc |
| Banco      | PostgreSQL 16 (prod) · H2 in-memory (perfil `dev`)   |
| Frontend   | Next.js 15 · React 19 · TypeScript 5 strict · Tailwind |
| Container  | Docker Compose                                       |
| Testes     | JUnit 5 + Mockito (back) · Vitest (front)            |

---

## Pré-requisitos

- **Java 21** (`$JAVA_HOME=/opt/jdk-21` no laptop do workshop)
- **Maven 3.9+** (`$HOME/apache-maven-3.9.6/bin/mvn`)
- **Node 20+** (via `nvm install 20`)
- **Docker + Docker Compose** (opcional para subir tudo junto)

---

## Subir local — modo rápido (sem Docker)

Recomendado para o workshop: backend em H2 in-memory, frontend em hot-reload.

```bash
# 1. Backend (perfil dev = H2 in-memory, Flyway desabilitado)
JAVA_HOME=/opt/jdk-21 $HOME/apache-maven-3.9.6/bin/mvn \
  spring-boot:run -Dspring-boot.run.profiles=dev
# → http://localhost:8080
# → http://localhost:8080/swagger-ui.html
# → http://localhost:8080/h2-console (jdbc:h2:mem:sifap, user=sa)

# 2. Frontend (em outro terminal)
cd frontend
npm install
npm run dev
# → http://localhost:3001
```

---

## Subir local — modo Docker Compose

Sobe Postgres + backend + frontend + executa Flyway automaticamente.

```bash
docker compose up --build
# → backend  : http://localhost:8080
# → frontend : http://localhost:3001
# → postgres : 127.0.0.1:5432 (db=sifap, user=sifap, pwd=sifap_local)
```

Variáveis sobrescrevíveis via `.env` na raiz: `POSTGRES_DB`, `POSTGRES_USER`,
`POSTGRES_PASSWORD`, `SIFAP_JWT_SECRET`.

---

## Rodar testes

```bash
# Backend (32 testes JUnit 5 + Mockito)
JAVA_HOME=/opt/jdk-21 $HOME/apache-maven-3.9.6/bin/mvn test

# Frontend (Vitest)
cd frontend && npm test
```

---

## Endpoints principais

| Método | Path                                  | REQ-ID(s)                  |
|--------|---------------------------------------|----------------------------|
| GET    | `/api/v1/beneficiaries`               | REQ-BEN-001..006           |
| POST   | `/api/v1/beneficiaries`               | REQ-BEN-001..006           |
| GET    | `/api/v1/admin/programs`              | REQ-ADM-001..004           |
| POST   | `/api/v1/admin/programs`              | REQ-ADM-004 (FATOR-K)      |
| POST   | `/api/v1/payments/cycle/{yyyy-MM}`    | REQ-PAY-001..005           |
| GET    | `/api/v1/audit`                       | REQ-AUD-001, REQ-AUD-002   |
| GET    | `/swagger-ui.html`                    | OpenAPI 3 docs (springdoc) |
| GET    | `/actuator/health`                    | health check               |

---

## Estrutura do projeto

```
.
├── pom.xml                          # Maven (Java 21 + Spring Boot 3.3)
├── Dockerfile                       # Backend (build multi-stage)
├── docker-compose.yml               # postgres + backend + frontend
├── src/main/java/com/sifap/
│   ├── beneficiary/                 # bounded context beneficiary
│   ├── admin/                       # bounded context admin (programas)
│   ├── payment/                     # bounded context payment
│   ├── audit/                       # bounded context audit
│   └── common/                      # CorsConfig, CpfUtils, MoneyUtils
├── src/main/resources/
│   ├── application.yml              # default (PostgreSQL + Flyway)
│   ├── application-dev.yml          # H2 in-memory
│   ├── application-docker.yml       # PostgreSQL via docker-compose
│   └── db/migration/
│       ├── V1__init_schema.sql      # schema inicial (4 contextos)
│       └── V2__audit_immutable.sql  # trigger append-only (REQ-AUD-001)
├── src/test/java/...                # 32 testes
├── frontend/                        # Next.js 15 (4 telas)
│   ├── src/app/
│   │   ├── beneficiaries/page.tsx
│   │   ├── payments/page.tsx
│   │   ├── programs/page.tsx
│   │   └── audit/page.tsx
│   ├── src/lib/cpf.ts               # CPF mod-11 (REQ-BEN-006 bypass)
│   └── Dockerfile
└── specs/002-geracao-ciclo-pagamento/  # spec ativa
```

---

## Rastreabilidade

- **Spec mestre:** [`02-spec-moderna/SPECIFICATION.md`](02-spec-moderna/SPECIFICATION.md) — 17 REQ-IDs
- **Regras de negócio:** [`01-arqueologia/business-rules-catalog.md`](01-arqueologia/business-rules-catalog.md) — BR-001..015
- **Mistérios do legado:** [`01-arqueologia/mysteries-found.md`](01-arqueologia/mysteries-found.md) — MYS-001..010
- **ADRs:** `02-spec-moderna/ADRs/` (monólito modular, persistência, auth)

---

## Status de entrega (Persona 3 — Technical Lead + Developer)

- [x] 17 REQ-IDs implementados (4 bounded contexts)
- [x] CPF mod-11 validation (back + front)
- [x] CORS configurado (`localhost:3000/3001`)
- [x] 32/32 testes verdes
- [x] Flyway V1 (schema) + V2 (audit immutable trigger)
- [x] springdoc-openapi (`/swagger-ui.html`)
- [x] Dockerfiles + docker-compose corrigidos
- [x] README operacional

— Time Rosa-02
