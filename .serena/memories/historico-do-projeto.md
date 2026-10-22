# Histórico do projeto — resumo

Histórico completo, com datas reais do git e tom narrativo, está em
`docs/HISTORICO-DO-PROJETO.md` — ler esse arquivo pra contexto de "por que isso existe"
antes de assumir que um comportamento é bug.

## Marcos principais
- 21/05/2026: MVP nasce (telnet real, parser VT100, licença, Room DB).
- 13/07/2026: três repos (app, admin, relay) unificados num monorepo.
- 11/08/2026: ambientes sandbox/produção separados — permite testar sem afetar clientes.
- 22–24/09/2026: grande unificação (ScanTE Browser/Config separados, v1.1.0, nasce o
  ScanTE Monitor a partir da cerca digital, v1.1.1 "versão Centauro").
- 23–25/09/2026: única visita à Centauro com registro real no banco (coletor Honeywell,
  `25351B041D`). Nada registrado depois disso.
- 01/10/2026: fixes "(Centauro)" — reconexão após ERROR, teclado físico do coletor,
  tratamento de NEL (ESC E) no `TerminalEmulator`. Esse fix de NEL nunca foi confirmado
  rodando no aparelho real (só testado depois do último acesso registrado).
- 14/10/2026: multi-host desbloqueado quando a empresa não pede lock de conexão.
- 20/10/2026: alarme sonoro customizável (BEL), app verifica atualização sozinho.
- 21/10/2026 em diante: pesquisa de concorrentes (Ivanti Velocity/JLT) + protótipo de
  reformatação de tela pra Centauro, numa branch separada
  (`feature/reformatacao-tela-poc`, nunca mesclada na main). Ver `mem:centauro-status-atual`.
