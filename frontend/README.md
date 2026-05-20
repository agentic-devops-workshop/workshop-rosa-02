# SIFAP 2.0 — Frontend

Next.js 15 (App Router) + React 19 + TypeScript 5 (strict) + Tailwind CSS.

## Telas (mapeamento → bounded context)

| Rota | Bounded Context | REQ-IDs cobertos |
|------|-----------------|------------------|
| `/beneficiaries` | beneficiary | REQ-BEN-001..006 |
| `/payments` | payment | REQ-PAY-001..005 |
| `/programs` | admin | REQ-ADM-001..004 |
| `/audit` | audit | REQ-AUD-001, REQ-AUD-002 |

## Desenvolvimento

```bash
npm install
npm run dev    # http://localhost:3001
```

A variável `NEXT_PUBLIC_API_BASE_URL` aponta para o backend (default `http://localhost:8080`).

## Tipagem

- `tsconfig.json` com `strict: true` — sem exceções.
- Path alias `@/* → ./src/*`.
- `npx tsc --noEmit` deve passar com 0 erros antes de commit.
