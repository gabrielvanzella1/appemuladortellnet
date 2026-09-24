# Cenários de teste — Centauro

Status conforme a legenda do [README](README.md). "Obtido" registra só o que foi observado; o que não foi executado fica PENDENTE.

## ScanTE Emulador Telnet 1.1.0

### EMU-01 — Instalação/atualização no CK62
- **Objetivo:** confirmar que a versão corrigida está no equipamento.
- **Pré-requisitos:** APK `ScanTE-1.1.0-20260923.apk`.
- **Procedimento:** instalar por cima da versão antiga; abrir o app; conferir a versão.
- **Esperado:** app abre na versão 1.1.0, mantendo a licença.
- **Obtido (23/09):** parceiro instalou; tela mostra 1.1.0, licença ativa, empresa Centauro.
- **Status:** OK
- **Evidência:** print enviado pelo parceiro em 23/09 (arquivar — ver [evidencias.md](evidencias.md)).

### EMU-02 — Ativação da licença e registro no painel
- **Objetivo:** o coletor ativado aparece no painel da empresa.
- **Procedimento:** ativar com a chave da Centauro; abrir Dispositivos no painel.
- **Esperado:** coletor listado com versão 1.1.0 e licença ativa.
- **Obtido (24/09):** banco de produção lista `Honeywell CK62 | 1.1.0 | ativa`, último acesso 24/09 14:15:46.
- **Status:** OK
- **Evidência:** consulta ao banco (ver evidencias.md).

### EMU-03 — Licença presa ao aparelho
- **Objetivo:** uma chave já usada não ativa outro aparelho.
- **Procedimento:** tentar ativar a chave do CK62 em outro aparelho.
- **Esperado:** recusa com "Licença vinculada a outro dispositivo".
- **Obtido (24/09):** recusado no emulador com essa mensagem (a validação é a mesma API usada pelo Emulador e pelo Monitor).
- **Status:** OK-EMULADOR

### EMU-04 — Conexão Telnet ao host da Centauro
- **Pré-requisitos:** endereço e porta do host Telnet/WMS da Centauro. [NAO CONFIRMADO]
- **Procedimento:** criar a conexão; conectar.
- **Esperado:** tela do host aparece, com cores e posição corretas.
- **Obtido:** —
- **Status:** PENDENTE

### EMU-05 — Login automático
- **Pré-requisitos:** EMU-04; usuário de teste da Centauro.
- **Procedimento:** configurar usuário/senha na conexão; conectar.
- **Esperado:** o app responde aos prompts de login sozinho.
- **Status:** PENDENTE

### EMU-06 — Leitura de código de barras no CK62
- **Procedimento:** com uma tela de entrada aberta, bipar um código.
- **Esperado:** o código entra no campo, igual à digitação.
- **Status:** PENDENTE

### EMU-07 — Tamanho da tela e teclas do teclado ScanTE
- **Contexto:** o parceiro reclamou da versão antiga (tamanho de tela e botões do teclado faltando); corrigido na 1.1.0.
- **Obtido (23/09):** o desenvolvedor conferiu na 1.1.0 e aprovou; falta conferir no CK62 com uma tela real da Centauro.
- **Status:** PARCIAL

### EMU-08 — Queda e volta do Wi-Fi durante a sessão
- **Objetivo:** registrar o comportamento real.
- **Procedimento:** com sessão aberta, desligar o Wi-Fi por 30 s e religar.
- **Esperado:** [NAO CONFIRMADO] — o comportamento do Emulador nesse caso ainda não foi documentado a partir do código. Registrar o que acontecer (a sessão cai? reconecta? exige novo login?).
- **Status:** PENDENTE

### EMU-09 — Impressão ESC/POS
- Só se a Centauro usar impressora no coletor. [NAO CONFIRMADO]
- **Status:** N/A até confirmação

## ScanTE Monitor 1.0.0

### MON-01 — Instalação
- **Procedimento:** instalar o APK de release.
- **Esperado:** app "ScanTE Monitor" instalado, abre na tela de ativação.
- **Obtido (24/09):** OK no emulador.
- **Status:** OK-EMULADOR

### MON-02 — Ativação com a chave da empresa
- **Procedimento:** digitar a chave (o app formata com hífens) e o nome do coletor; tocar em Ativar.
- **Esperado:** "Painel: monitoramento ATIVO para a empresa".
- **Obtido (24/09):** ativado com uma chave trial da Centauro; mensagem exibida.
- **Status:** OK-EMULADOR
- **Evidência:** `MON-02_ativacao_ok.png`

### MON-03 — Recusa de chave de outro aparelho
- **Obtido (24/09):** a chave do CK62 foi recusada no emulador: "Licença vinculada a outro dispositivo". O CK62 não foi afetado.
- **Status:** OK-EMULADOR

### MON-04 — Permissões
- **Procedimento:** seguir os avisos do app: localização → "Permitir o tempo todo" (abre a tela do Android) → notificações → localização do Android ligada → sem restrição de bateria.
- **Esperado:** todos os itens com ✓ na tela de status.
- **Obtido (24/09):** ✓ em localização, "o tempo todo", localização ligada e notificações. **"Sem restrição de bateria" ficou ✗** no emulador.
- **Obtido (24/09, 2ª rodada):** com as permissões concedidas pelo adb (`pm grant` + lista de exceção de bateria), os 5 itens ficaram ✓. O fluxo pela tela para "sem restrição de bateria" segue não validado.
- **Status:** PARCIAL — repetir no MC33 (ver pendências)
- **Evidência:** `MON-04_permissao_tempo_todo.png`, `MON-05_sinal_enviado.png`

### MON-05 — Sinal completo chega ao painel
- **Esperado:** o painel recebe roteador Wi-Fi (BSSID/SSID), localização e bateria.
- **Obtido (24/09, emulador):** gravado em produção: SSID `AndroidWifi`, BSSID `00:13:10:85:fe:01`, sinal −50 dBm, GPS com precisão de 5 m, bateria 100%.
- **Status:** OK-EMULADOR
- **Evidência:** `MON-05_sinal_enviado.png` + consulta ao banco

### MON-06 — Coletor sem GPS (MC33)
- **Objetivo:** confirmar o caso real da Centauro.
- **Esperado:** zona identificada pelo roteador Wi-Fi; localização por rede se o Android fornecer; sem GPS.
- **Status:** PENDENTE — só é possível no MC33

### MON-07 — Um só dispositivo com o Emulador
- **Objetivo:** Monitor e Emulador no mesmo coletor aparecem como uma linha no painel.
- **Obtido (24/09):** com os dois APKs de release (mesma assinatura), o device_id foi idêntico (`9e95c22c…`).
- **Status:** OK-EMULADOR
- **Observação:** só vale para APKs de **release**; um APK debug gera outro device_id.

### MON-08 — Liga sozinho ao ligar o coletor
- **Procedimento:** reiniciar o coletor; não abrir o app; verificar a notificação do Monitor.
- **Obtido (24/09):** após o reinício do emulador, o serviço subiu em primeiro plano sem abrir o app.
- **Status:** OK-EMULADOR

### MON-09 — Trava por senha
- **Procedimento:** fechar e reabrir o app; digitar senha errada; digitar a senha certa (padrão `1234`).
- **Esperado:** abre bloqueado; senha errada → "Senha incorreta"; senha certa libera o status e os botões.
- **Obtido (24/09):** exatamente o esperado.
- **Status:** OK-EMULADOR
- **Evidência:** `MON-09_abre_bloqueado.png`, `MON-09_senha_incorreta.png`, `MON-09_liberado.png`

### MON-10 — Trocar a senha do coletor
- **Procedimento:** desbloquear; Trocar senha do coletor → `4321` → Salvar; fechar e reabrir; tentar `1234`, depois `4321`.
- **Esperado:** nova senha (4 a 8 dígitos) passa a valer; `1234` deixa de funcionar.
- **Obtido (24/09):** `1234` → "Senha incorreta"; `4321` → libera.
- **Status:** OK-EMULADOR
- **Evidência:** `MON-10_senha_antiga_recusada.png`, `MON-10_senha_nova_aceita.png`

### MON-11 — Desativar o monitoramento
- **Procedimento:** desbloqueado (com a senha), Desativar monitoramento → Desativar.
- **Esperado:** para o serviço e volta à tela de ativação.
- **Obtido (24/09):** voltou à tela de ativação, com o campo de chave vazio; `dumpsys` com 0 serviços do Monitor rodando. A confirmação não pede a senha de novo: a senha é exigida para chegar ao botão.
- **Status:** OK-EMULADOR
- **Evidência:** `MON-11_desativado.png`

### MON-12 — Operador tenta parar pelo Android
- **Procedimento:** Configurações do Android → Apps → ScanTE Monitor → Forçar parada / Desinstalar.
- **Esperado (limitação conhecida):** **é possível**. A senha protege só as telas do app. Impedir isso exige o modo Device Owner. [PLANEJADO]
- **Status:** PENDENTE — registrar para a apresentação

### MON-13 — Sem internet
- **Procedimento:** tirar a internet do coletor por alguns minutos (manter o Wi-Fi local, se possível); devolver.
- **Esperado:** os sinais do período chegam ao painel depois que a internet volta (fila de até 200 leituras).
- **Obtido (24/09, emulador ativado na conta de demonstração):** Wi-Fi e dados desligados das 19:06:01 às 19:09:32 UTC. O app mostrou "Último envio falhou: Unable to resolve host · 1 na fila". As leituras de 19:06:42, 19:07:42 e 19:08:42 chegaram juntas às 19:09:44 (sem roteador, porque o Wi-Fi estava desligado).
- **Observação:** não foi possível testar "Wi-Fi ligado, mas sem internet" no emulador.
- **Status:** OK-EMULADOR
- **Evidência:** `MON-13_sem_internet_fila.png` + consulta ao banco

## Painel da empresa (ScanTE Admin)

### PAN-01 — Login da Centauro
- **Esperado:** entra no painel da empresa; menu com Cerca digital.
- **Status:** PENDENTE (fazer antes da apresentação)

### PAN-02 — Dispositivos
- **Obtido (24/09):** CK62 listado com versão 1.1.0 e licença ativa.
- **Status:** OK

### PAN-03 — Cerca: roteadores e zonas
- **Procedimento:** Cerca digital → Pontos de acesso; marcar "na cerca" os roteadores do CD e dar nome de zona.
- **Esperado:** coletores passam a aparecer na zona do roteador ao qual estão conectados.
- **Obtido (23/09, ambiente LOCAL com coletores simulados):** telas funcionando.
- **Status:** PENDENTE na Centauro (os roteadores deles só aparecem depois que um coletor com o Monitor se conectar)
- **Evidência:** `PAN-cerca_pontos_LOCAL.png`, `PAN-cerca_visao_geral_LOCAL.png`

### PAN-04 — Alerta "sem comunicação"
- **Esperado:** abre após 15 min sem sinal, só dentro do expediente.
- **Obtido:** coberto pelo teste automatizado local `tools/testar_cerca.php` (34 verificações passando); não visto em produção com aparelho real.
- **Status:** PENDENTE em produção

### PAN-05 — Alerta de bateria baixa
- **Esperado:** abre abaixo de 15% **e** sem carregar.
- **Status:** PENDENTE em produção (coberto no teste local)

### PAN-06 — Alerta fora da cerca
- **Esperado:** abre quando o coletor se conecta a um roteador não marcado como "na cerca".
- **Status:** PENDENTE em produção (coberto no teste local)

### PAN-07 — Linha do tempo do coletor
- **Esperado:** por onde o coletor passou no dia, por zona, com duração e bateria.
- **Obtido (23/09, LOCAL simulado):** tela funcionando.
- **Status:** PENDENTE em produção
- **Evidência:** `PAN-cerca_linha_tempo_LOCAL.png`

### PAN-08 — Alterar senha do painel
- **Status:** PENDENTE
