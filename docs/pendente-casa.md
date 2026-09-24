# Para fazer no PC de casa

Situação em 24/09/2026. A apresentação da Centauro (25/09) usa a versão **1.1.0 que já está no CK62**. Nada daqui é necessário para ela.

## O que aconteceu

O app unificado (1.1.0) foi montado em 23/09 juntando:
- o repositório do trabalho (`gabrielvanzella1/appemuladortellnet`);
- o **EmuladorCompleto do GitHub**, que parou em **29/07** (commit `b3d5de9`, botão ⊞ de ocultar teclas).

O que foi feito no PC de casa **depois de 29/07 e não subiu** ficou de fora. Exemplo: o `TellX.apk` de 10/08, "app com calculadora", que tinha o botão 🖩 na sessão.
Referência: [assets/screenshots/referencia-calculadora-versao-casa.png](../assets/screenshots/referencia-calculadora-versao-casa.png).

## Passo 1 — Salvar o trabalho de casa, antes de tudo

No repositório **EmuladorCompleto** do PC de casa:

```
git status
git add -A
git commit -m "wip: trabalho de casa depois de 29/07 (calculadora na sessao etc.)"
git push origin HEAD:casa-pendente
```

- Vai para um ramo separado (`casa-pendente`), **não** para o `main`.
- Antes do `git add`, conferir que nenhuma keystore, `.jks` ou senha vai junto.

## Passo 2 — Pegar o código unificado e juntar

```
git clone https://github.com/gabrielvanzella1/appemuladortellnet.git scante-unificado
cd scante-unificado
git remote add casa https://github.com/GabrielVanzella/EmuladorCompleto.git
git fetch casa
```

Depois, pedir ao Claude: *"trazer o ramo casa/casa-pendente para o main sem perder nada do que já está no main; conferir o botão da calculadora e o tamanho das teclas"*.
Conflito esperado em `EmuladorTelnet/.../MainActivity.kt` e `res/layout/activity_main.xml`: os dois lados mexeram na barra do topo.

## Passo 3 — Calculadora na sessão

- **Já existe no unificado:** a calculadora flutuante menor e responsiva (`FloatingCalculatorHelper`, 17/09) e `abrirCalculadora()` em `MainActivity`, que envia o resultado ao terminal. Hoje ela só abre pelo menu de opções (tecla MENU).
- **Falta:** o botão 🖩 na barra do topo, entre ⊞ (`keysToggle`) e o teclado (`keyboardToggle`), visível só quando conectado.
- Se o ramo de casa trouxer o botão, use o dele e não crie outro, senão aparecem dois.

## Passo 4 — Tamanho das teclas

- Hoje, na barra de teclas do terminal: altura **44dp** (`MainActivity.kt`, `val btnH = (44 * density)`), texto 11sp. Na versão do trabalho era 36dp.
- Conferir no coletor. Se o código de casa tiver outro valor, fica o que você aprovou.

## Passo 5 — Testar e gerar a versão nova

- Versão sugerida: **1.1.1, versionCode 4**. Motivo: restaura a calculadora e ajusta as teclas (correção, sem funcionalidade nova). O versionCode 3 já foi usado pela 1.2.0 de teste (não distribuída).
- **A chave de assinatura fica no PC do trabalho** (`C:\Users\7700924385\ScanTE-Chaves-Assinatura\`). Sem ela, o APK de release não atualiza o app do CK62 (vai exigir desinstalar). Levar por pendrive, **nunca pelo git**, junto com o `keystore.properties`.
- Depois de testar: atualizar `docs/homologacao/centauro/cenarios.md` (EMU-07) e o `MANUAL_ScanTE.md`.

## Outros pendentes

- A pasta principal do trabalho (`appEmuladorTellnet`) ainda está na estrutura antiga (admin como submódulo). Atualizar com cuidado. Este repositório (`main`) já tem tudo.
- O repositório é **público** e tem a homologação da Centauro em `docs/homologacao/`. Avaliar deixá-lo privado (GitHub → Settings → Danger Zone → Change visibility).
- Resto: [docs/homologacao/centauro/pendencias.md](homologacao/centauro/pendencias.md).
