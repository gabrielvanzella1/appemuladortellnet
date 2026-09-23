# Cerca Wi-Fi — admin ScanTE (v1)

Data: 23/09/2026 · Status: aprovado pelo dono do produto · Escopo: só o admin (o app que envia os
sinais vem numa versão futura — provavelmente o "ScanTE Monitor", app separado, com Device Owner).

## Problema
A Centauro tem ~500 coletores Android **sem GPS**, que trabalham **dentro do CD**. Querem saber
onde eles estão e ser avisados quando saem da área.

## Ideia
A cerca é **a rede Wi-Fi do CD**: cada coletor informa o roteador (AP/BSSID) em que está conectado.
Os roteadores são descobertos sozinhos; a empresa marca quais fazem parte da cerca e dá nome à zona.
Coletor sem chip que sai do prédio perde o Wi-Fi — a saída aparece como "sem comunicação", com o
último roteador visto (o ponto de saída).

## Estados do coletor (calculados na leitura)
- **dentro** — último AP está na cerca (mostra a zona)
- **fora_da_cerca** — conectado a um AP/rede que não é da cerca
- **sem_comunicacao** — último sinal mais antigo que o limite da empresa (só dentro do expediente)
- **fora_do_expediente** — sem sinal, mas fora do horário (não alerta)
- **sem_cerca** — empresa ainda não marcou nenhum AP (mostra só o AP)
- **sem_dados** — nunca enviou sinal

## Alertas (`dispositivo_alertas`, com início e fim)
- `fora_da_cerca`: abre quando um sinal chega de AP fora da cerca; fecha quando volta a um AP da cerca.
- `sem_comunicacao`: o estado atual é calculado na leitura (sem cron); quando o coletor volta após
  um intervalo > limite, o período é gravado com o último AP visto.
- `bateria_baixa`: abre com bateria < limite e sem carregar; fecha ao carregar ou ao passar de limite+5%.

## Banco (migration `2026_09_cerca_wifi.sql`)
- `empresas`: `rastreamento_ativo` (padrão 0), `limite_sem_comunicacao_min` (15), `limite_bateria_pct` (15),
  `expediente_inicio` (06:00), `expediente_fim` (22:00), `expediente_dias` ("1,2,3,4,5,6", ISO 1=seg).
- `dispositivos`: estado atual (último sinal, BSSID, SSID, RSSI, bateria, carregando, lat/lng) e
  `chave_licenca` VARCHAR(32) (a 24 cortava as chaves de 25).
- `pontos_acesso`: roteadores por empresa (BSSID único por empresa), SSID, zona, `na_cerca`.
- `dispositivo_sinais`: histórico (90 dias, limpo em lotes no próprio envio — sem cron). Já aceita
  lat/lng/precisão para coletores com GPS no futuro.
- `dispositivo_alertas`: alertas com início/fim.
- Horários gravados em UTC (como o resto do banco); convertidos para America/Sao_Paulo na exibição.

## API
`POST /api/dispositivo/sinal` (mesma autenticação `Bearer API_SECRET` do ping)
```json
{ "device_id": "…", "sinais": [ { "capturado_em": 1758650000, "bssid": "aa:bb:cc:dd:ee:ff",
  "ssid": "CENTAURO-CD", "rssi": -61, "bateria": 80, "carregando": false,
  "lat": null, "lng": null, "precisao": null } ] }
```
Resposta: `{ "ok": true, "rastreamento": true|false, "intervalo_seg": 120 }`. Só grava se o
dispositivo pertence a uma empresa com `rastreamento_ativo = 1`. Até 200 sinais por envio, em ordem
de captura. O ping passa a responder também `rastreamento` (o app antigo ignora).

## Telas
- **Painel da empresa → "Cerca digital"** (menu só aparece com rastreamento ligado):
  visão geral (contadores, coletores por zona, alertas abertos, lista de coletores),
  linha do tempo do dia por coletor, pontos de acesso (marcar cerca + zona), alertas (histórico),
  configurações (limites e expediente).
- **Admin → empresa**: liga/desliga o rastreamento.

## Teste
Simulador CLI (`tools/simular_cerca.php`, nunca publicado no servidor) manda sinais pela API para
coletores `SIM-*` numa empresa de teste. Em produção: empresa "Scan TE Produção"; a Centauro fica
desligada até o app estar pronto.

## Fora do escopo (v1)
Planta do CD com APs posicionados, e-mail de alerta (precisa de cron), cerca em círculo para GPS,
o app que envia os sinais.
