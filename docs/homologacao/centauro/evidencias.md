# Evidências — Centauro

Prints ficam em [`assets/screenshots/homologacao-centauro/`](../../../assets/screenshots/homologacao-centauro/).
Regra: nenhuma evidência pode mostrar chave de licença inteira, senha ou segredo. Print com chave aparente não entra no repositório. Nesse caso, a evidência fica registrada em texto.

| Teste | Data | Tipo | Onde / conteúdo |
|---|---|---|---|
| EMU-01 | 23/09 | Print do parceiro (CK62) | **Arquivar** — mostra 1.1.0 ativa Centauro |
| EMU-02 / PAN-02 | 24/09 | Consulta ao banco de produção | `Honeywell CK62 · 1.1.0 · ativa · último acesso 2026-09-24 14:15:46` |
| EMU-03 / MON-03 | 24/09 | Texto (o print mostra a chave inteira, por isso não foi copiado) | Monitor no emulador: "Licença vinculada a outro dispositivo" |
| MON-02 | 24/09 | Print | `MON-02_ativacao_ok.png` |
| MON-04 | 24/09 | Print | `MON-04_permissao_tempo_todo.png` |
| MON-05 | 24/09 | Print + banco | `MON-05_sinal_enviado.png`; banco: `AndroidWifi · 00:13:10:85:fe:01 · -50 dBm · 37.421998,-122.084000 ±5 m · bateria 100 · 2026-09-24 13:07:35` |
| MON-07 | 24/09 | Banco + assinatura | Os dois APKs de release com o mesmo certificado (SHA-256 `c8eef2b7…`); o sinal do Monitor chegou com o device_id do Emulador (`9e95c22c…`) |
| MON-08 | 24/09 | `dumpsys` | `ServiceRecord{… scantemonitor/.MonitorService} isForeground=true` após reiniciar, sem abrir o app |
| MON-09 | 24/09 | Prints | `MON-09_abre_bloqueado.png`, `MON-09_senha_incorreta.png`, `MON-09_liberado.png` |
| MON-10 | 24/09 | Prints | `MON-10_senha_antiga_recusada.png`, `MON-10_senha_nova_aceita.png` |
| MON-11 | 24/09 | Print + `dumpsys` | `MON-11_desativado.png`; 0 serviços do Monitor após desativar |
| MON-13 | 24/09 | Print + banco | `MON-13_sem_internet_fila.png`; banco: leituras capturadas às 19:06:42, 19:07:42 e 19:08:42, recebidas às 19:09:44 (rede voltou às 19:09:32) |
| Demo | 24/09 | Execução do script | `scripts/preparar-demo-cerca.ps1 -Minutos 1`: 8 coletores e 5 zonas recriados; 181 sinais de histórico por coletor (161 no SIM-08); 1 rodada ao vivo |
| PAN-03 | 23/09 | Print LOCAL (simulado) | `PAN-cerca_pontos_LOCAL.png`, `PAN-cerca_visao_geral_LOCAL.png` |
| PAN-07 | 23/09 | Print LOCAL (simulado) | `PAN-cerca_linha_tempo_LOCAL.png` |
| PAN-04..06 | 23/09 | Teste automatizado local | `scante-admin/tools/testar_cerca.php` — 34 verificações passando (ferramenta local, não vai para produção) |

## Limpeza após os pré-testes (24/09)

As duas licenças trial da Centauro usadas no emulador foram desvinculadas, e os registros do emulador foram apagados. Conferido depois: a Centauro tem só o CK62 real no painel e as 5 licenças trial estão livres.

Na 2ª rodada (tarde de 24/09), outras limpezas:
- A licença trial da conta de demonstração, usada no emulador, foi liberada, e os registros do emulador foram apagados.
- O roteador virtual do emulador (`AndroidWifi 00:13:10:85:fe:01`) foi removido dos Pontos de acesso da Centauro e da conta de demonstração. A 1ª limpeza não tinha pegado essa tabela.

Conferido depois: nenhum resto do emulador em produção.
