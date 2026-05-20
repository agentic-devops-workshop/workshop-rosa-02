# ADR-002 — Estratégia de Integração com Banco do Brasil e SIAFI

**Data:** 20/05/2026
**Status:** Aceita
**Decisores:** Par 2 (EA + SA) · revisão Par 5 (DevOps)
**Origem no legado:** BATCHCON.NSN (integração BB via CNAB 240) · DDM PAGAMENTO campo DT-EMISSAO/DT-CONFIRMACAO

---

## Contexto

O SIFAP legado integra com dois sistemas financeiros críticos:

1. **Banco do Brasil** — recebe arquivo CNAB 240 no D-1, retorna confirmação/devolução no D+1.
   Identificado em BATCHCON.NSN, com layout CNAB 240 documentado desde 2008.
2. **SIAFI** — sistema de execução financeira federal. Campo `integracao-siafi` no DDM PAGAMENTO
   indica integração adicionada em 2002 via Webservice SOAP.

Cada integração tem características diferentes de risco, protocolo e contrato:
- BB: protocolo de arquivo batch maduro, contrato estável há 18+ anos.
- SIAFI: SOAP implantado em 2002, contrato governamental de alta rigidez — qualquer mudança
  exige coordenação com outro ministério.

## Opções Consideradas

### Integração com Banco do Brasil

#### Opção A: Manter CNAB 240 via SFTP (escolhida)
- **Descrição:** Manter o protocolo atual de transferência de arquivo CNAB 240 por SFTP.
- **Vantagens:** Contrato já homologado pelo BB, sem necessidade de certificação nova,
  zero risco de regressão no fluxo principal.
- **Desvantagens:** Processo assíncrono (D-1/D+1), sem confirmação em tempo real.

#### Opção B: REST API do BB (Open Finance)
- **Descrição:** Usar a API REST moderna do BB para submissão de pagamentos em lote.
- **Vantagens:** Mais moderno, resposta mais rápida.
- **Desvantagens:** Exige novo processo de certificação com o BB, fora do escopo do workshop,
  risco real de quebrar o fluxo de pagamento de 4,2 milhões de beneficiários.

### Integração com SIAFI

#### Opção A: Manter SOAP existente (escolhida)
- **Descrição:** Wrapper Java que consome o Webservice SOAP do SIAFI sem alteração de contrato.
- **Vantagens:** Contrato governamental inalterado, sem coordenação com SIAFI necessária.
- **Desvantagens:** SOAP é tecnologia legada, mas contrato estável.

#### Opção B: Substituir por REST
- **Descrição:** Migrar para eventual API REST do SIAFI.
- **Vantagens:** Tecnologia mais moderna.
- **Desvantagens:** SIAFI não disponibiliza REST para este contrato. Inviável no prazo.

## Decisão

**Para Banco do Brasil:** manter protocolo **CNAB 240 via SFTP**.
- Encapsular em `payment/infrastructure/BankFileExporter` e `BankReturnImporter`.
- O formato CNAB é detalhe de infraestrutura — o domínio `payment` não conhece CNAB.

**Para SIAFI:** manter **Webservice SOAP** via wrapper Java.
- Encapsular em `payment/infrastructure/SiafiClient` (interface em `domain/`).
- Se o SIAFI estiver indisponível, gravar evento na fila de reprocessamento e alertar operador.

## Justificativa

Ambas as integrações têm contratos governamentais estabelecidos. A troca de protocolo exige
processo de certificação fora do escopo do workshop e representa risco real para o fluxo de
pagamento. O princípio do Strangler Fig se aplica aqui: encapsular os adaptadores de integração
permite substituição futura sem alterar o domínio.

## Consequências

- **Positivas:** Zero risco de quebra de integração; encapsulamento permite evolução futura.
- **Negativas:** CNAB 240 é processo assíncrono — sem confirmação imediata de pagamento.
- **Risco operacional:** Se o BB rejeitar o arquivo CNAB (ex.: layout errado), o ciclo inteiro
  falha. Mitigação: validação do arquivo antes do envio + alert para operador.
- **Quando revisitar:** Quando o BB disponibilizar e homologar API REST para pagamentos
  governamentais em lote, ou quando o SIAFI migrar para REST.
