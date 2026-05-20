# ADR-002 — Estratégia de integração financeira e externa

- **Status:** Aceita
- **Data:** 20/05/2026
- **Decisores:** Par 2 — Enterprise Architect + Software Architect

## Contexto

O legado SIFAP depende de integrações externas para fechar o ciclo de pagamento e a conciliação.
As evidências do repositório apontam dois vínculos relevantes:

- remessa bancária para o Banco do Brasil via arquivo CNAB 240
- conciliação financeira com o SIAFI em fluxo legado batch

Também existem validações documentais que podem demandar integração externa, mas elas não são o núcleo operacional do ciclo P0.

Ao mesmo tempo, a spec vigente define que parte das integrações permanece fora do escopo funcional da v1.0, o que impede assumir uma implementação completa de todas elas nesta etapa.

## Decisão

Adotar uma estratégia de integração por adaptadores de saída, preservando os contratos operacionais legados sem espalhar detalhes de protocolo pelo domínio.

Diretriz por integração:

1. **Banco do Brasil**
   O módulo `payment` expõe uma porta de remessa e retorno. A implementação inicial deve suportar geração e processamento de arquivos compatíveis com o fluxo CNAB 240 usado pelo legado.

2. **SIAFI**
   O sistema deve reservar uma porta de conciliação financeira no contexto `payment`, mas a implementação funcional completa pode ser faseada após o núcleo P0, conforme o escopo v1.0 da spec.

3. **Validação documental externa**
   Qualquer integração de CPF ou documento deve ficar atrás de um contrato próprio, sem contaminar a lógica de elegibilidade e cadastro com detalhes técnicos externos.

## Consequências

### Positivas

- O domínio permanece estável mesmo se o protocolo externo mudar.
- O Par 3 consegue implementar o núcleo de pagamento sem acoplar regra de negócio a parser de arquivo.
- A evolução futura do SIAFI não obriga refatoração ampla do core.

### Negativas

- Exige modelagem explícita de portas e DTOs de integração.
- Pode haver trabalho inicial adicional para desenhar contratos antes do código operacional.
- O time precisa distinguir claramente o que é porta pronta e o que é integração realmente implementada na v1.0.

## Regras de implementação

1. O domínio não conhece layout de arquivo nem formato externo.
2. Os adaptadores de integração ficam em infraestrutura.
3. Falhas externas não devem corromper o estado interno já persistido sem política de compensação.
4. Eventos relevantes de remessa, retorno e conciliação devem produzir auditoria.

## Alternativas consideradas

### Implementar integrações diretamente dentro de services de domínio

Rejeitada porque mistura protocolo externo com regra de negócio e dificulta testes.

### Adiar qualquer contrato de integração para depois do workshop

Rejeitada porque deixaria o módulo `payment` sem fronteira explícita para remessa e conciliação, dificultando o handoff técnico ao Par 3.

## Relação com a spec

- Sustenta REQ-PAY-005 e o fluxo batch do contexto `payment`.
- Preserva a restrição operacional herdada do legado para remessa e conciliação.
- Complementa [C4-DIAGRAMS.md](C4-DIAGRAMS.md) ao posicionar Banco do Brasil e SIAFI como sistemas externos.