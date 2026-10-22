# ScanTE — visão geral do projeto

Suite de apps Android + painel PHP pra conectar coletores de código de barras (rugged
handhelds) a sistemas corporativos via Telnet/VT100 (Protheus/TOTVS, AS/400 e outros),
mais rastreamento de frota e persistência de sessão.

## Repositório (monorepo desde 13/07/2026)
- `EmuladorTelnet/` — app Android principal (ScanTE Emulador Telnet), Kotlin. Flavors
  `sandbox` e `production` (ver `EmuladorTelnet/app/build.gradle.kts`).
- `scante-admin/` — painel administrativo PHP (licenças, empresas, versões do app,
  cerca digital, chamados).
- `scante-relay/` — programa Windows que mantém sessão do coletor viva durante quedas
  de Wi-Fi.
- `scante-browser/`, `scante-config/`, `scante-monitor/` — apps Android irmãos nascidos
  de features que saíram do Emulador (modo Browser, config remota, cerca digital por
  Wi-Fi sem GPS).
- `docs/` — infraestrutura (`INFRA.md`), homologação por cliente (`homologacao/`),
  histórico completo em `HISTORICO-DO-PROJETO.md` (`mem:historico-do-projeto` tem o
  resumo).

## Cliente principal em homologação: Centauro
Ver `mem:centauro-status-atual` pro estado em aberto.

## Como rodar
`run.ps1` builda e instala no dispositivo conectado (flavor sandbox por padrão).
Emulador Android não roda bem nesta máquina (i5-8265U/pouca RAM) — testar sempre em
aparelho físico (Honeywell EDA52 é o principal de testes).
