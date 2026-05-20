<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# 002 — Geração de Ciclo de Pagamento

## Artefatos
- `spec.md`: requisitos EARS com `source_legacy`.
- `plan.md`: plano técnico da implementação.
- `research.md`: decisões e ambiguidades levantadas.
- `data-model.md`: entidades e regras de integridade.
- `contracts/openapi.yaml`: contrato inicial da API do ciclo.
- `tasks.md`: backlog técnico sequenciado com testes antes do código.
- `quickstart.md`: validação manual do fluxo principal.

## Checklist de pronto
- [x] Pasta da funcionalidade segue `NNN-nome-curto`.
- [x] Requisitos legados têm `source_legacy:` com `.NSN`.
- [x] Requisito greenfield tem justificativa `[GREENFIELD]`.
- [x] `tasks.md` prioriza testes antes de implementação.
- [x] `quickstart.md` permite validação manual do fluxo principal.

## Próximos passos (Spec-Kit)
1. Rodar `/speckit.clarify` para fechar as ambiguidades de `research.md`.
2. Rodar `/speckit.analyze` para detectar inconsistências de spec/plan/tasks.
3. Submeter para sign-off do PO antes da passagem #2.
