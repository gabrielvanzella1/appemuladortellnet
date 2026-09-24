# Resultado — Centauro

**Situação em 24/09/2026 (véspera da apresentação): EM ANDAMENTO.**

## Placar

| Área | OK | OK-EMULADOR | PARCIAL | FALHOU | PENDENTE | N/A |
|---|---|---|---|---|---|---|
| Emulador (EMU-01..09) | 2 | 1 | 1 | 0 | 4 | 1 |
| Monitor (MON-01..13) | 0 | 10 | 1 | 0 | 2 | 0 |
| Painel (PAN-01..08) | 1 | 0 | 0 | 0 | 7 | 0 |
| **Total** | **3** | **11** | **2** | **0** | **13** | **1** |

Nenhum teste falhou até agora. O Monitor passou em tudo que foi testado (inclusive trocar senha, desativar e ficar sem internet), mas **só no emulador**. O caso real (MC33 sem GPS) ainda não foi testado.

**Preparação da apresentação (24/09):** APK do Monitor em Downloads; a assinatura do ScanTE do CK62 é igual à do Monitor (o Monitor ativa no CK62 com a mesma licença); script da conta de demonstração testado em produção; restos do emulador removidos da Centauro e da conta de demonstração. Roteiro em [roteiro-apresentacao.md](roteiro-apresentacao.md).

## O que pode ser mostrado na apresentação

- CK62 com o Emulador 1.1.0 ativo e registrado no painel (EMU-01, EMU-02).
- Monitor: ativação, trava por senha, início automático no boot e sinal chegando ao painel (no emulador).
- Telas da cerca digital com dados **simulados** (visão geral, pontos de acesso, linha do tempo). Deixar claro que são simulados.

## O que não pode ser afirmado ainda

- Que o Monitor funciona no MC33: falta testar (P-02).
- Que o operador não consegue desligar o Monitor: ele consegue pelo Android até existir o modo Device Owner (P-07).
- Comportamento do Emulador na queda de Wi-Fi (EMU-08).

## Parecer

_A preencher após os testes no ambiente da Centauro._

| Campo | Valor |
|---|---|
| Parecer | APROVADO / APROVADO COM RESSALVAS / REPROVADO |
| Data | |
| Responsável ScanTE | |
| Responsável Centauro | |
| Ressalvas | |
