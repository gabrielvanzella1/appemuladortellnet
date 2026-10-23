# Gravador de telas (ScanTE 1.1.3) — para continuar de casa

Implementação criada na branch `feature/gravador-telas`, a partir de `50f4aee` (versão **1.1.3 / versionCode 6**). Em 22/10/2026, essa branch foi integrada localmente à `feature/reformatacao-tela-poc` para preparar a demonstração. As duas funcionalidades continuam sendo frentes conceitualmente separadas: o gravador captura sessões reais; a PoC de reformatação é um formulário fixo para um servidor simulado.

## O que é
Função do app (**Configurações → Gravação de telas**: Iniciar / Pausar / Retomar / Parar e enviar; selo ● REC no topo da sessão) que grava a conversa com o host Telnet num
arquivo JSON, para depois montar a "tela gráfica" a partir de telas reais (ideia do Ivanti Velocity:
gravar → editar numa ferramenta → carregar de volta no coletor). Hoje existe só a **primeira peça (o gravador)**.

| Peça | Estado |
|---|---|
| Gravador no app | **Feito e testado no emulador (22/10)** |
| Editor (no painel admin) | Não começou |
| Motor genérico no app (lê as telas de um arquivo) | Não começou — o protótipo `reformatacao-tela-poc` tem uma tela fixa no código |

## O que o arquivo guarda (`scante-gravacao-telas`, versão 1)
Eventos em ordem, cada um com `t_ms`:
- `tela`: `linhas` (24 textos), `cursor [linha,coluna]`, `campos` ([linha,coluna,tamanho] de áreas em vídeo
  reverso/cor de fundo = campos de preenchimento), `destaques` (trechos com cor/negrito/sublinhado/pisca).
  Só grava quando a tela muda e fica parada ~0,4 s.
- `tecla`: nome da tecla especial (ENTER, TAB, F1…F12, setas, CTRL+X…).
- `digitou`: **só a quantidade de caracteres** — o texto digitado NUNCA é gravado.
- `alarme_host`: o host mandou BEL (0x07) — mostra se o sistema avisa erro por som.

**Privacidade:** as telas mostram o que o HOST exibiu, então códigos/usuário aparecem como eco; a senha só aparece
se o host a mostrar sem máscara. Conferir o arquivo antes de compartilhar; usar usuário de teste.

## Onde está o código
- `terminal/ScreenRecorder.kt` — gravador (captura com debounce, máscara das teclas, autosave a cada 15 eventos).
- `terminal/TerminalEmulator.kt` — `snapshot()` e `onBellRaw` (BEL mesmo com alarme silenciado); `feed()` ficou `@Synchronized`.
- `ui/TelnetViewModel.kt` — liga o gravador em `addTerminalOutput`, `sendRaw`, `sendCommand` e no BEL.
- `GravacaoActivity.kt` + `res/layout/activity_gravacao.xml` — tela Configurações → Gravação de telas (iniciar/pausar/parar, salvar, cópia em Downloads, compartilhar via FileProvider).
- `MainActivity.kt` — registra a sessão em primeiro plano no gravador (`onResume`) e mostra o selo ● REC / ⏸ REC (toque abre a tela de gravação).
- `ScreenRecorder.instance` — gravador ÚNICO no app (vale para a sessão em primeiro plano; com 2 sessões abertas os eventos se misturam).
- `AndroidManifest.xml` + `res/xml/file_paths.xml` — FileProvider (`<applicationId>.fileprovider`).
- Arquivos ficam em `Android/data/com.logisticapp.emuladortelnet/files/gravacoes/`.

## Ensaio com coletor real (ex.: EDA52) usando o SHARK simulado
1. PC e coletor no mesmo Wi-Fi. No PC: `python scripts/shark_simulado.py 2300 0.0.0.0` e liberar a porta 2300 no firewall.
2. No coletor: instalar o APK, ativar, cadastrar host = **IP do PC** (`ipconfig`), porta 2300.
3. Configurações → Gravação de telas → Iniciar; voltar à sessão; login (qualquer), `1`, bipar `7891000100103` (válido), `999` (inválido → bipe + "PRODUTO INVALIDO") → Parar.
4. Conferir o JSON: telas, `alarme_host` no código inválido, e nenhuma senha no texto.

## Build oficial
`cd EmuladorTelnet; .\gradlew.bat assembleProductionRelease` → conferir com `apksigner verify --print-certs`:
SHA-256 deve começar com `c8eef2b7`. Precisa do `keystore.properties` + `.jks` (fora do git — ver `docs/INFRA.md`).

## Próximos passos (ordem sugerida)
1. Ensaio no EDA52 e ajustes do gravador.
2. Receber a gravação real da Centauro (Urovo) → ver telas, erros e se o SHARK manda BEL.
3. Editor no painel: carregar o JSON, mostrar cada tela como grade, marcar identificação/campos/botões/erros, gerar o arquivo de telas.
4. Motor genérico no app lendo esse arquivo (substitui a tela fixa do protótipo) + ler a resposta do host para mostrar erro/sucesso e tocar o som.
