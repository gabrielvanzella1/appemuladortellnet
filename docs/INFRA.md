# appemuladortelnet — notas de infra

## App Android (EmuladorTelnet / TellX)
- Flavors: `sandbox` (default do `run.ps1`) e `production`. Ver `EmuladorTelnet/app/build.gradle.kts`.
- Build debug funciona em qualquer máquina: `.\run.ps1 -Flavor production` (ou `assembleProductionDebug`).
- Build **release** (assinado) exige `EmuladorTelnet/keystore.properties` (nunca versionado,
  ver `.gitignore`). Sem esse arquivo o release fica sem assinatura. Template em
  `EmuladorTelnet/keystore.properties.example` — copie o `.jks` real e preencha os 4 campos.
  **Importante:** tem que ser a MESMA keystore usada nos releases anteriores (Centauro etc.),
  nunca gerar uma nova — isso quebra update pra quem já instalou.

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
