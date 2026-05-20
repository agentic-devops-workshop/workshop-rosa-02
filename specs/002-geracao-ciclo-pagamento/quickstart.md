<!-- markdownlint-disable MD013 MD025 MD026 MD028 MD029 MD034 MD040 MD051 MD060 -->

# Quickstart — 002 Geração de Ciclo de Pagamento

## Objetivo
Validar manualmente o fluxo principal de geração de ciclo mensal com as regras críticas da spec.

## Pré-condições
- Backend em execução local.
- Base com beneficiários de teste cobrindo status e regiões.
- API disponível em `http://localhost:8080`.

## Cenário 1 — Geração de ciclo com filtro por status (`REQ-PAY-001`)
1. Preparar massa com 10 beneficiários `A` e 2 `S`.
2. Executar criação do ciclo para competência `202605`.
3. Verificar que apenas 10 pagamentos foram gerados.

Resultado esperado:
- `generatedPayments=10`
- `rejectedBeneficiaries=2`

## Cenário 2 — Elegibilidade especial região 99 (`REQ-PAY-002`)
1. Usar beneficiário com `regionCode=99` e documentação válida.
2. Rodar avaliação no contexto do ciclo.
3. Verificar resultado elegível sem validações padrão.

Resultado esperado:
- beneficiário incluído no ciclo mesmo com ausência de critérios padrão.

## Cenário 3 — Teto de desconto não judicial (`REQ-PAY-003`)
1. Definir pagamento bruto de `1000.00`.
2. Aplicar descontos não judiciais somando `350.00`.
3. Executar cálculo.

Resultado esperado:
- desconto não judicial aplicado = `300.00`.

## Cenário 4 — Exceção desconto judicial (`REQ-PAY-004`)
1. Definir pagamento bruto de `1000.00`.
2. Aplicar desconto judicial de `500.00` e não judicial de `400.00`.
3. Executar cálculo.

Resultado esperado:
- desconto total = `800.00` (judicial `500.00` + não judicial truncado em `300.00`).

## Cenário 5 — Validações cadastrais (`REQ-BEN-*`)
1. Testar status inválido `X` no cadastro.
2. Testar CPF `11111111111`.
3. Testar CPF `00012345678`.
4. Testar inclusão de dependente para status `C`.
5. Testar inclusão do 6º dependente.

Resultado esperado:
- status inválido rejeitado;
- CPF repetitivo rejeitado;
- prefixo especial segue exceção;
- bloqueio para status `C`;
- bloqueio no 6º dependente.
