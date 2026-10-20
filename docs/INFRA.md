# appemuladortelnet — notas de infra

## App Android (EmuladorTelnet / TellX)
- Flavors: `sandbox` (default do `run.ps1`) e `production`. Ver `EmuladorTelnet/app/build.gradle.kts`.
- Build debug funciona em qualquer máquina: `.\run.ps1 -Flavor production` (ou `assembleProductionDebug`).
- Build **release** (assinado) exige `EmuladorTelnet/keystore.properties` (nunca versionado,
  ver `.gitignore`). Sem esse arquivo o release fica sem assinatura. Template em
  `EmuladorTelnet/keystore.properties.example` — copie o `.jks` real e preencha os 4 campos.
  **Importante:** tem que ser a MESMA keystore usada nos releases anteriores (Centauro etc.),
  nunca gerar uma nova — isso quebra update pra quem já instalou.
- **Chave oficial:** certificado SHA-256 `c8eef2b78a8898017cb4edacb409ce49f221cc9eeeaeec4db660259f95583e73`.
  Todo APK release deve passar em `apksigner verify --print-certs` com esse valor antes de ir pra coletor.
- **Assinar no PC de casa:** copiar `scante-release.jks` (fora do repo, ex.: `C:\Users\<user>\ScanTE-Chaves-Assinatura\`)
  e o `keystore.properties` para `EmuladorTelnet/` e `scante-monitor/`, trocando só a linha `storeFile`.
  Gerar com `.\gradlew.bat assembleProductionRelease`. Passo a passo completo foi entregue fora do git
  (`INSTRUCOES-CLAUDE-CASA.md`, 20/10/2026).
- **Atenção (20/10/2026):** o 1.1.1 instalado no CK62 da Centauro NÃO foi assinado com a chave oficial
  (ID do aparelho mudou de `42930dac…` para `7e088945…` em 25/09). A próxima atualização oficial exige
  desinstalar e liberar a licença no painel. Próxima versão sugerida: 1.1.2 / versionCode 5.

## Backend scante-admin (PHP, Hostinger)
- Produção: `https://scante.com.br/scante-admin/public`. Config real em
  `scante-admin/config/config.php` (gitignored, só existe no servidor).
- Deploy: servidor tem clone sparse-checkout (`~/repo-appemuladortelnet`, só a pasta
  `scante-admin`) + script `~/deploy-scante-admin.sh` (git pull + rsync, preserva
  `config/config.php` e `public/uploads/`). Pra deployar: `ssh <host-hostinger> "./deploy-scante-admin.sh"`.
- Acesso SSH: chave `~/.ssh/scante_deploy`, host configurado em `~/.ssh/config` do Gabriel
  (alias local, não versionado — cada máquina configura o próprio).

## Git
- `origin` (GabrielVanzella/EmuladorCompleto) — sem permissão de push da conta usada aqui.
- `old-origin` (gabrielvanzella1/appemuladortellnet) — remoto ativo, é pra onde os pushes vão.

## Cliente Centauro
- Ver memória do projeto `[[centauro-adaptations]]`: adaptações pra Centauro não se limitam a
  commits com "Centauro" no texto — incluem fixes de reconexão e teclado físico do coletor
  feitos na mesma janela de trabalho.
- **Multi-host (commit `ba76d72`):** sessões novas só travam Host/Porta se a empresa tiver o
  bloqueio de conexão ativo no painel (`CompanyConfigStore.isConnectionLocked`). Centauro está
  com esse bloqueio desligado (`lock_conexao_ativo=0`, confirmado no banco em 14/10), então já
  consegue cadastrar hosts diferentes e alternar entre eles. Testado de ponta a ponta no EDA52
  com dois hosts reais (`towel.blinkenlights.nl` e `mapscii.me`).
- **Som de alerta do alarme do host (commit `5bcf86b`):** `AlarmSoundPlayer` toca o alarme BEL
  no stream de **ALARME** (não notificação) de propósito — ignora "Não perturbe" por padrão, sem
  precisar de permissão especial. Em Configurações > Emulação > VT Opções > "Som do alarme do
  host" dá pra escolher entre 3 alertas prontos (sintetizados, sem precisar de arquivo), toque do
  sistema, ou importar um arquivo de áudio próprio. Cada opção toca prévia ao tocar na lista.
  **Assume que o host da Centauro manda BEL (0x07) quando o código é inválido** — confirmado pelo
  usuário em 20/10, não testado contra o ERP real deles ainda.
- **Assinatura:** essas duas mudanças (e tudo até aqui) seguem em build **debug** — a keystore
  oficial ainda não chegou no PC de casa (ver seção acima). O CK62 da Centauro já está rodando
  numa versão não assinada oficialmente; manter a mesma situação por ora não piora nada, só não
  resolve o problema de assinatura que já existia.
