# Guia Didatico — Seu Papel em Arquitetura (Par 2)

Este guia foi feito para voce que acumula as duas personas de arquitetura:

- Enterprise Architect (EA)
- Software Architect (SA)

Objetivo: deixar claro o que voce faz, em qual etapa, qual entregavel produzir e como saber se esta no caminho certo.

## 1) Visao rapida do seu papel no processo

Voce e o ponto de conversao entre descoberta e implementacao.

- Voce recebe insumos do Estagio 1 (regras e evidencias do legado).
- Voce transforma isso em especificacao executavel no Estagio 2 (EARS, ADRs, C4, limites de contexto).
- Voce guarda a integridade estrutural no Estagio 3 (evitar acoplamento indevido entre contextos).
- Voce valida continuidade e governanca no Estagio 4 (issues/PRs do Agent sem quebrar arquitetura).

Sem o Par 2 bem executado, o Estagio 3 vira implementacao por adivinhacao.

## 2) O que pertence a cada persona

### Enterprise Architect (EA)

Foco principal:

- Fronteira do sistema no ecossistema
- Integracoes externas e contratos
- C4 Nivel 1 (System Context)
- ADRs de topologia e estrategia de coexistencia

Pergunta que guia o EA:

- "Como o SIFAP 2.0 se encaixa com sistemas externos sem quebrar contratos?"

### Software Architect (SA)

Foco principal:

- Arquitetura interna do monolito modular
- Bounded contexts e fronteiras
- C4 Niveis 2 e 3
- ADR de modularidade e regras de dependencia entre modulos

Pergunta que guia o SA:

- "Como organizar o codigo para manter evolucao com baixo acoplamento?"

## 3) Atividades por etapa (SDLC)

## Estagio 1 — Arqueologia (apoio com foco arquitetural)

Objetivo do Par 2 nesta etapa:

- Ajudar a transformar leitura do legado em mapa de contexto e dependencias.

Atividades praticas:

- Ler programas Natural e DDMs para identificar fronteiras de dominio.
- Levantar integracoes externas visiveis no legado (ex.: SIAFI, BB, sistemas internos).
- Apoiar montagem do dependency-map com visao de arquitetura.
- Listar riscos de integracao para preparar ADRs do Estagio 2.

Entregaveis influenciados por voce:

- 01-arqueologia/dependency-map.md
- Insumos para 01-arqueologia/discovery-report.md

Sinal de qualidade:

- O mapa explica claramente quem conversa com quem e por qual motivo de negocio.

## Estagio 2 — Spec Moderna (lideranca do Par 2)

Objetivo do Par 2 nesta etapa:

- Converter descoberta em direcao tecnica clara e testavel.

Atividades praticas do EA:

- Definir C4 Nivel 1.
- Registrar ADRs estruturais (topologia, integracao, estrategia de modernizacao).
- Explicitar trade-offs e opcao rejeitada em cada ADR.

Atividades praticas do SA:

- Definir bounded contexts.
- Produzir C4 Nivel 2 (containers) e Nivel 3 (componentes-chave).
- Definir regra de fronteira entre modulos (o que pode ou nao pode importar/chamar).
- Preparar estrutura base para o time de implementacao.

Entregaveis obrigatorios:

- Requisitos EARS com source_legacy em 02-spec-moderna/
- ADRs (minimo recomendado no guia: 3)
- Diagramas C4 L1/L2 (e L3 quando fizer sentido)

Sinal de qualidade:

- Implementacao consegue iniciar sem ambiguidade de fronteiras.
- Cada requisito tem rastreabilidade (source_legacy ou GREENFIELD justificado).

## Estagio 3 — Implementacao (guarda da arquitetura)

Objetivo do Par 2 nesta etapa:

- Garantir que o codigo entregue respeita a arquitetura decidida.

Atividades praticas:

- Revisar PRs com foco em fronteiras de contexto.
- Barrar acoplamento indevido entre modulos.
- Apoiar TL/Dev quando houver conflito entre velocidade e consistencia arquitetural.
- Validar com DBA impacto das fronteiras no desenho de dados.

Checklist de revisao arquitetural em PR:

- O modulo novo respeita contexto de negocio?
- Ha importacao cruzando fronteira sem contrato explicito?
- A decisao contradiz ADR aprovado?
- Existe risco de virar monolito por camada tecnica em vez de feature?

Sinal de qualidade:

- Codigo cresce por contexto de negocio, nao por pastas genericas.

## Estagio 4 — Evolucao com Agent (governanca e sustentabilidade)

Objetivo do Par 2 nesta etapa:

- Assegurar que automacao com Agent nao degrade arquitetura.

Atividades praticas:

- Revisar issues delegadas para Agent sob lente de impacto arquitetural.
- Revisar PRs gerados por Agent e exigir alinhamento com ADR/C4.
- Validar implicacoes de infraestrutura junto ao Par 5.

Sinal de qualidade:

- Evolucao continua rapida sem quebrar modularidade.

## 4) Handoffs (passagens) em que voce e protagonista

## H1 — Legado para Spec (fim do Estagio 1)

Voce recebe:

- Regras de negocio catalogadas
- Mapa de dependencias
- Evidencias do legado

Sua acao:

- Filtrar o que impacta arquitetura e transformar em decisoes de spec.

## H2 — Spec para Codigo (fim do Estagio 2)

Voce entrega para Par 3 e Par 4:

- Fronteiras claras de contexto
- ADRs aprovadas
- C4 suficiente para implementar
- Regras de dependencia entre modulos

Este e o handoff mais critico do seu papel.

## H3 — Codigo para Ops (fim do Estagio 3)

Voce apoia:

- Garantia de coerencia entre arquitetura e pipeline/infra.

## 5) Riscos comuns do seu papel (e como evitar)

Risco 1: desenho bonito, mas nao executavel

- Como evitar: toda decisao arquitetural precisa virar criterio de PR e estrutura concreta.

Risco 2: ambiguidade entre EA e SA

- Como evitar: EA cuida de fora para dentro; SA de dentro para fora.

Risco 3: modularidade so no diagrama

- Como evitar: revisar imports/dependencias em PR com checklist fixo.

Risco 4: specs sem rastreabilidade

- Como evitar: bloquear requisito sem source_legacy ou GREENFIELD justificado.

## 6) Plano de trabalho sugerido para voce (timebox)

## Bloco A (inicio do Estagio 1)

- 20 min: mapear integracoes e fronteiras no legado.
- 20 min: consolidar riscos arquiteturais para o Estagio 2.

## Bloco B (Estagio 2)

- 20 min: C4 L1 + rascunho ADR de topologia.
- 20 min: bounded contexts + C4 L2.
- 20 min: refinamento L3 essencial + regras de fronteira + handoff H2.

## Bloco C (Estagio 3)

- Ciclo continuo: revisar PRs estruturais, responder duvidas de fronteira.

## Bloco D (Estagio 4)

- Revisao de impacto arquitetural em issues/PRs do Agent.

## 7) Definicao de pronto do seu papel

Voce concluiu bem seu papel quando:

- O time implementa sem duvida sobre fronteiras.
- Nao ha violacoes arquiteturais recorrentes em PR.
- ADRs explicam decisao, trade-off e alternativa rejeitada.
- C4 e compreendido rapidamente por quem nao e especialista.

## 8) Referencias internas para consulta

- 00-TEAM-FLOW.md
- 01-arqueologia/GUIDE.md
- 02-spec-moderna/GUIDE.md
- 05-personas/03-enterprise-architect/PERSONA.md
- 05-personas/04-software-architect/PERSONA.md
- 08-exemplos/ADR-001-monolito-modular-exemplo.md
- 08-exemplos/SPECIFICATION-exemplo.md

## 9) Acao imediata recomendada (agora)

1. Revisar o dependency-map e discovery-report do Estagio 1.
2. Definir seus 3 ADRs principais do Estagio 2 (titulo e decisao).
3. Desenhar C4 L1/L2 com foco no que habilita implementacao imediata.
4. Preparar checklist de revisao arquitetural para usar nos PRs do Estagio 3.
