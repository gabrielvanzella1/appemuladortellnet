# Homologação — Centauro

Registro da homologação da Suite ScanTE no cliente **Centauro**.

| Item | Valor |
|---|---|
| Cliente | Centauro (empresa id 5 no painel) |
| Apresentação | 25/09/2026 |
| Pré-testes | 23 e 24/09/2026 |
| Responsável | Gabriel Vanzella |
| Status geral | **EM ANDAMENTO** — ver [resultado.md](resultado.md) |

## Escopo

| Produto | Versão | Por que está no escopo |
|---|---|---|
| ScanTE Emulador Telnet | 1.1.0 (versionCode 2) | App de operação — já instalado e ativado no CK62 |
| ScanTE Monitor | 1.0.0 (versionCode 1) | Controle da frota de ~500 coletores Zebra MC33 **sem GPS** (localização por roteador Wi-Fi, bateria, trava por senha) |
| ScanTE Admin (painel da empresa) | produção | Dispositivos, licenças e cerca digital |

**Fora do escopo desta homologação:** ScanTE Browser, ScanTE Config e ScanTE Relay.
Motivo: não há confirmação de que a Centauro usa sistema web no coletor ou host Telnet fora do CD. [NAO CONFIRMADO]

## Equipamentos

| Equipamento | Identificação | Uso | Status |
|---|---|---|---|
| Honeywell CK62 | nome no painel `25351B041D`, device_id `42930dac…` | Emulador 1.1.0, licença ativa da Centauro | Em uso pelo parceiro |
| Zebra MC33 (sem GPS) | — | Frota da Centauro (~500) — alvo do Monitor | **Aparelho para teste não confirmado** [NAO CONFIRMADO] |
| Emulador Android (AVD `TestDevice`, Android 14) | device_id `9e95c22c…` (release) | Pré-testes do Monitor | Usado em 24/09 |

## Ambiente

- Painel e API: ScanTE Admin em produção (Hostinger).
- Empresa Centauro com **cerca digital ligada**.
- Licenças da Centauro: 1 ativa (CK62) + 5 trial livres. As chaves não são registradas aqui.
- Padrões da cerca (código: `migrations/2026_09_cerca_wifi.sql`): alerta "sem comunicação" após **15 min**, bateria baixa em **15%**, expediente **06:00–22:00, seg–sáb**.
- Monitor (código: `MonitorService.kt`): leitura a cada **60 s** (5 min se a cerca da empresa estiver desligada), re-registro no painel a cada ~30 leituras, fila local de até **200** sinais.

## Arquivos

| Arquivo | Conteúdo |
|---|---|
| [checklist.md](checklist.md) | Checklist reutilizável para qualquer coletor |
| [cenarios.md](cenarios.md) | Casos de teste com procedimento, esperado e obtido |
| [evidencias.md](evidencias.md) | Onde está cada evidência |
| [pendencias.md](pendencias.md) | O que falta, com dono |
| [resultado.md](resultado.md) | Placar e parecer final |
| [roteiro-apresentacao.md](roteiro-apresentacao.md) | Passo a passo da apresentação de 25/09 |
| [../../monitor/manual.md](../../monitor/manual.md) | Manual do ScanTE Monitor (instalação, cerca, senha, problemas comuns) |
| [../../../scripts/preparar-demo-cerca.ps1](../../../scripts/preparar-demo-cerca.ps1) | Reativa os coletores simulados da conta de demonstração |

## Legenda de status

| Status | Significado |
|---|---|
| OK | Executado no equipamento/ambiente de homologação e passou |
| OK-EMULADOR | Passou no emulador Android; falta repetir no equipamento real |
| PARCIAL | Parte validada, parte não |
| FALHOU | Executado e não passou |
| PENDENTE | Ainda não executado |
| N/A | Não se aplica à Centauro |
