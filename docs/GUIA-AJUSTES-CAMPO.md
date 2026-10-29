# Guia de ajustes em campo (Centauro) — atualizado em 29/10/2026

Para quem precisa mexer no app durante a visita, de qualquer PC. Leia junto com [EM-ANDAMENTO.md](EM-ANDAMENTO.md).

## 1. Estado de partida

- Branch: `feature/reformatacao-tela-poc` (remoto `old-origin`; o `origin` é outro repositório, não usar).
- Versão: **1.1.4 (versionCode 7)**. Último ajuste: `432912d` — fila única de envio ao host.
- APK que está no coletor Urovo da Centauro: variante `production` debug, arquivo `ScanTE-1.1.4-fix-ordem.apk`
  (SHA-256 `8DC9FBA1EF6FB4671A25EF26027E228E1D3B31712F69A3F2CBC7425968FADBA8`).
  Assinatura debug `47fb9332cf6968af4947fc58736c48aa680e40875ab7ab0504c4c4ebe69438e0`.
- Esse APK **não** tem o código de log de diagnóstico. A receita está na seção 5.
- Os APKs não ficam no Git. Estão no PC de desenvolvimento em `Desktop\centauro23-10` e `Desktop\para-fabricio`.

## 2. Configuração de referência do coletor (Urovo, o que "está funcionando")

- Host `wmascltprd08`, porta `1024`; terminal `vt220`; terminador de linha `CR`; fonte 20.
- Wi-Fi `Coletores_CD`: o host responde (~38 ms), mas o `scante.com.br` **foi bloqueado**. Ativar a licença por outra
  rede (hotspot do celular) e depois voltar ao `Coletores_CD`.
- O leitor de código de barras do Urovo entrega o código como texto e em seguida um Enter sintético (KEYCODE_ENTER,
  device -1). A configuração do uScanner do aparelho **não foi lida**; conferir em coletor novo.
- Licença: uma chave por aparelho (fica presa ao primeiro que ativar). Chaves não vão para o Git.

## 3. Onde mexer (mapa rápido)

Pasta base: `EmuladorTelnet/app/src/main/java/com/logisticapp/emuladortelnet/`

| Assunto | Arquivo |
|---|---|
| Envio ao host (fila única, ordem dos bytes) | `ui/TelnetViewModel.kt` → `sendRaw` / `txQueue` |
| Teclas físicas (Enter, setas, F1–F9, ESC) | `terminal/TerminalView.kt` → `handleKeyEvent` |
| Leitor de código de barras (broadcast) e ação após leitura | `BarcodeScannerManager.kt`, `MainActivity.kt` → `sendBarcodeToServer` |
| Reconexão / tela apagada / estado da conexão | `MainActivity.kt` → `observeViewModel`, `registerScreenOffReceiver` |
| Leitura do host e IAC/Telnet | `network/TelnetClient.kt` → `readData` |
| Até 8 hosts e botão de troca (1/3) | `SessionStore.kt`, `MainActivity.kt` → `updateSessionBadge` |
| Terminador de linha | `MainActivity.kt` → `lineTerminatorBytes` (vem de Telnet Opções) |
| Variantes (`production`, `sandbox`, `recorder`) | `app/build.gradle.kts` |

## 4. Compilar e instalar

No PowerShell, dentro de `EmuladorTelnet`:

```powershell
.\gradlew.bat assembleProductionDebug          # APK: app\build\outputs\apk\production\debug\app-production-debug.apk
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"   # adb NÃO está no PATH
& $adb devices -l                               # com 2+ aparelhos, usar -s <serial>
& $adb install -r caminho\do\apk                # NUNCA desinstalar: apaga licença e dados
```

**Armadilha de assinatura:** todo PC tem a sua própria `debug.keystore`. Se o APK for gerado em outro PC, o Android
recusa a instalação por cima ("signatures do not match") e a única saída é desinstalar, o que **apaga a licença**.
Para ajustar em outro PC, copiar `%USERPROFILE%\.android\debug.keystore` do PC original por um meio seguro
(nunca pelo Git) e conferir antes: `apksigner verify --print-certs <apk>` deve mostrar `47fb9332…`.
O app oficial assinado (SHA `c8eef2b7…`) depende da keystore do repositório privado de chaves.

## 5. Diagnóstico com o coletor no USB

Ler configuração do app instalado (build debug): `& $adb shell run-as com.logisticapp.emuladortelnet cat shared_prefs/app_settings.xml`
(não ler nem copiar o arquivo `*.license.xml`: contém a chave).

Receita do log que achou o erro de ordem (apagar depois: registra o que a operadora digita). Arquivo novo `IoLog.kt`:

```kotlin
object IoLog {
    fun i(msg: String) { if (BuildConfig.DEBUG) android.util.Log.i("SCANTE_IO", msg) }
    fun esc(b: ByteArray, len: Int = b.size) = buildString {
        for (k in 0 until minOf(len, b.size)) { val c = b[k].toInt() and 0xFF
            if (c in 32..126) append(c.toChar()) else append("<%02x>".format(c)) }
    }
}
```

Pontos de inserção: `TelnetViewModel.sendRaw` (antes de enfileirar e depois de gravar em `txQueue`), `TelnetClient.readData`
(logo após `input.read`: `IoLog.i("RX n=$n: ${IoLog.esc(buf, minOf(n,300))}")`), `TerminalView.handleKeyEvent`
(keyCode, deviceId, source), `BarcodeScannerManager` (action do broadcast), `MainActivity` (estado da conexão e tela apagada).
Capturar: `& $adb logcat -c; & $adb logcat -v time -s SCANTE_IO:I > log.txt`.
Ler: sequência `TX pedido` × `TX gravado` (deve estar na mesma ordem) e tempo entre `TX` e o próximo `RX`.

## 6. Em aberto

- **Atrasos nas setas da consulta** relatados pela operadora: **não reproduzidos**. Medido: app ~3 ms para gravar na rede,
  seta→resposta ~45 ms (só 6 amostras), rede Urovo→host 38 ms/0% perda. Causa desconhecida. Se voltar: anotar hora e tela,
  instalar a versão com log por cima (`install -r`) e capturar.
- **Versão oficial assinada (Production):** falta a keystore. Antes de gerar, reverter `LicenseActivity.kt:56`
  (`if (BuildConfig.DEBUG)` → `if (false && BuildConfig.DEBUG)`), que hoje mostra o botão de bypass na tela de licença
  (aparece só antes de ativar).
- Pesquisa de recursos do Zion: ver [PESQUISA-RECURSOS-ZION.md](PESQUISA-RECURSOS-ZION.md) (somente análise, nada implementado).

## 7. Lições (para não repetir)

- Nunca rodar `git reset --hard` com alterações não commitadas: foi perdido trabalho em 23/10. Recuperado de stash solto
  (`git fsck --lost-found --no-reflogs` + `git show <commit>:<arquivo>`). Commitar cedo.
- "Activity not exported" ao usar `adb shell am start -n …MainActivity` não é bug: só `LicenseActivity` é launcher.
  Abrir com `-n <pacote>/com.logisticapp.emuladortelnet.LicenseActivity`.
- O nome da classe usa o namespace `com.logisticapp.emuladortelnet` mesmo nas variantes `.recorder` e `.sandbox`.
