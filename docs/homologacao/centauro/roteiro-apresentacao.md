# Roteiro da apresentação — Centauro (25/09/2026)

Objetivo: mostrar o ScanTE rodando no CK62 da Centauro e o ScanTE Monitor controlando o coletor pelo painel, **com Wi-Fi e sem GPS**, que é o caso dos ~500 MC33 deles.

## Na véspera ou antes de sair

| # | Tarefa | Como |
|---|---|---|
| 1 | Instalar o **ScanTE Monitor** no CK62 | APK `Downloads\ScanTE-Monitor-1.0.0-20260924.apk` — passo a passo em [docs/monitor/manual.md](../../monitor/manual.md) |
| 2 | Ativar o Monitor com a **mesma chave do ScanTE do CK62** | O Monitor e o ScanTE têm a mesma assinatura: no CK62 os dois usam o mesmo ID e a mesma licença. **Não gasta licença trial** |
| 3 | Dar todas as permissões no CK62 | Tela de status do Monitor com 5 ✓ |
| 4 | Conferir os dois logins do painel | Centauro: `centauro@scante.com.br` · Demonstração: `cerca-teste@scante.com.br` |
| 5 | Levar | Notebook, CK62 carregado, celular com hotspot (para o "fora da cerca" ao vivo) |

> **Não use** `NAO-INSTALAR-ScanTE-1.2.0-20260924.apk`. O ScanTE certo é o `ScanTE-1.1.0-20260923.apk`, que já está no CK62.

## 10 minutos antes

1. No notebook, abra o PowerShell na pasta do projeto e rode:
   ```
   powershell -ExecutionPolicy Bypass -File .\scripts\preparar-demo-cerca.ps1
   ```
   **Deixe a janela aberta** o tempo todo (ela mantém os coletores simulados "vivos" por 120 min).
2. Conecte o CK62 no Wi-Fi da sala.
3. No painel da **Centauro**: Cerca digital → **Pontos de acesso**. O roteador da sala aparece sozinho depois de 1 ou 2 minutos. Marque "na cerca", dê o nome de zona **Sala de reunião** e toque em **Salvar cerca**.
4. Confira em **Visão geral**: CK62 como **Dentro**, na zona Sala de reunião, com a bateria.

## Ordem sugerida (~20 min)

### 1. O problema (2 min)
São ~500 coletores sem GPS. Hoje ninguém sabe em que área do CD cada um está, qual está sem bateria, qual sumiu ou parou de comunicar.

### 2. ScanTE no CK62 (5 min)
- Painel da Centauro → **Minhas Licenças / Dispositivos**: CK62 com licença ativa, versão 1.1.0 e último acesso.
- No coletor: tela de sessões e teclado ScanTE. Se tiver o host Telnet da Centauro, conectar. Se não, mostrar só a interface. [host NAO CONFIRMADO]

### 3. Monitor ao vivo no CK62 (6 min)
1. Barra de notificações: "ScanTE Monitor" sempre ligado.
2. Abrir o app: **pede senha**. Digitar uma errada ("Senha incorreta"), depois a certa. Mostrar a tela de status (roteador Wi-Fi, bateria, último envio).
3. Painel → **Cerca digital → Visão geral**: CK62 na zona **Sala de reunião**.
4. **Fora da cerca, ao vivo:** ligar o hotspot do celular e conectar o CK62 nele. Em 1 a 2 minutos o CK62 aparece como **Fora da cerca** e abre um alerta em **Alertas**. Voltar o CK62 para o Wi-Fi da sala: ele volta para **Dentro** e o alerta **fecha sozinho**.
5. **Linha do tempo** do CK62: por onde ele passou hoje.

### 4. Visão de frota (5 min) — conta de demonstração
Sair e entrar com `cerca-teste@scante.com.br`. **Dizer claramente que são dados simulados.**
- 5 zonas do CD (Recebimento, Estoque, Separação, Expedição, Doca) com os coletores em cada uma.
- Alertas: **bateria baixa** e **sem comunicação** (o coletor SIM-08 para de mandar sinal).
- **Linha do tempo** de um coletor no dia.

### 5. Próximos passos (2 min) — sem prometer data
- Testar no MC33 deles (pedir um emprestado).
- Travar o Monitor contra desinstalação (modo Device Owner) [PLANEJADO].
- Atribuir coletor a funcionário durante o turno [PLANEJADO].

## Perguntas para fazer à Centauro
1. Endereço/porta do host Telnet e um usuário de teste. (P-01)
2. Podem emprestar um MC33 para teste? Qual versão do Android? (P-02)
3. Quantos roteadores e quais áreas o CD tem? Existe planta com os APs? (P-03)
4. Usam sistema web no coletor, host fora do CD ou impressora no coletor? (P-04)
5. Usam algum MDM ou Zebra StageNow para instalar apps em massa? [NAO CONFIRMADO]

## O que NÃO afirmar

| Não diga | Por quê |
|---|---|
| "O operador não consegue desligar o Monitor" | Consegue, pelas configurações do Android. A senha protege só as telas do app |
| "Já está testado no MC33" | Só foi testado no CK62 e no emulador |
| Números de economia ou resultado | Não existem medições |
| "O Browser/Relay aparecem no painel" | Hoje só o ScanTE e o Monitor falam com o painel |

## Se algo der errado

| Problema | O que fazer |
|---|---|
| CK62 não aparece na cerca | Na tela do Monitor, conferir os 5 ✓ e o "Último envio"; tocar em **Enviar agora**. O roteador demora até 1 ou 2 minutos para aparecer em Pontos de acesso |
| Monitor mostra "monitoramento desativado para esta empresa" | Licença não vinculada ou cerca desligada: conferir em Licenças no painel |
| Sem internet na sala | Usar o hotspot do celular no CK62 e no notebook. A cerca passa a mostrar o hotspot como zona |
| Script de demonstração deu erro | Rodar de novo. Se não der, mostrar os prints em `assets/screenshots/homologacao-centauro/` |
| Coletores simulados todos "sem comunicação" | A janela do script foi fechada: rodar o script de novo |
