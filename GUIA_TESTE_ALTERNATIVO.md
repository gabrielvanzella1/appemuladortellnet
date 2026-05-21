# Testando APK - Alternativas Práticas

## ✅ APK Compilado com Sucesso!

**Localização**: `C:\Users\7700924385\web\trabalho\appEmuladorTellnet\EmuladorTelnet\app\build\outputs\apk\debug\app-debug.apk`

---

## 🚀 OPÇÃO 1: Android Studio (Mais Fácil)

1. Abrir Android Studio
2. File → Open → Selecionar pasta `EmuladorTelnet`
3. Esperar indexação completar
4. Criar/Iniciar Android Virtual Device (AVD):
   - Tools → Device Manager
   - Create Device
   - Selecionar API 34 (ou compatível)
   - Clicar Play para iniciar
5. Run → Run 'app'
6. Selecionar emulator na dialog

---

## 🎮 OPÇÃO 2: Comandos Diretos (Para Quem Quer CLI)

### Passo 1: Criar AVD se não existir
```powershell
$env:JAVA_HOME = "C:\Program Files\Amazon Corretto\jdk17.0.19_10"
$avdmanager = "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\cmdline-tools\bin\avdmanager.bat"

& $avdmanager create avd -n "TelnetTest" -k "system-images;android-34;google_apis;x86_64" --force
```

### Passo 2: Iniciar Emulator
```powershell
$emulator = "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe"
& $emulator -avd TelnetTest
```

Deixar esta janela aberta por 30-60 segundos para o emulator iniciar completamente.

### Passo 3: Em outra janela PowerShell - Instalar APK
```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

# Aguardar device conectar
& $adb wait-for-device

# Instalar APK
$apkPath = "C:\Users\7700924385\web\trabalho\appEmuladorTellnet\EmuladorTelnet\app\build\outputs\apk\debug\app-debug.apk"
& $adb install $apkPath

# Abrir app
& $adb shell am start -n "com.logisticapp.emuladortelnet/.MainActivity"
```

---

## 📱 OPÇÃO 3: Device Físico (Recomendado para Produção)

1. Conectar device Android via USB
2. Ativar "USB Debugging":
   - Ir em Configurações > Sobre o telefone
   - Tocar 7 vezes em "Número da Versão"
   - Ativar "Opções do Desenvolvedor"
   - Ativar "USB Debugging"
3. Executar:
```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$apkPath = "C:\Users\7700924385\web\trabalho\appEmuladorTellnet\EmuladorTelnet\app\build\outputs\apk\debug\app-debug.apk"

# Verificar conexão
& $adb devices

# Instalar
& $adb install $apkPath

# Abrir
& $adb shell am start -n "com.logisticapp.emuladortelnet/.MainActivity"
```

---

## 🧪 O Que Você Vai Ver

### Tela Inicial
```
┌────────────────────────────┐
│   EMULADOR TELNET          │
├────────────────────────────┤
│ Host: [_______________]    │
│ Port: [_____]              │
│ [Conectar] [Desconectar]   │
├────────────────────────────┤
│                            │
│ Terminal Output            │
│ (fundo preto, texto verde) │
│                            │
│ Status: Desconectado       │
└────────────────────────────┘
```

### Teste Básico
1. Digite um IP (ex: `telnet.towel.blinkenlights.nl`)
2. Clique "Conectar"
3. Observar:
   - Botão muda para "Desconectar"
   - Status muda para "Conectado"
   - Terminal exibe mensagens simuladas

---

## 🛠️ Troubleshooting

### "Emulator not found"
```powershell
# Verificar se existe
Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\emulator\"
```

### "System image not installed"
```powershell
$env:JAVA_HOME = "C:\Program Files\Amazon Corretto\jdk17.0.19_10"
$sdkmanager = "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\cmdline-tools\bin\sdkmanager.bat"
echo "y" | & $sdkmanager "system-images;android-34;google_apis;x86_64"
```

### "adb: command not found"
```powershell
# Verificar path
Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\platform-tools\"
```

### "Device offline" ou não aparece em `adb devices`
```powershell
# Reiniciar adb
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb kill-server
& $adb start-server
```

---

## 📊 Build Info

| Propriedade | Valor |
|------------|-------|
| APK Path | `app/build/outputs/apk/debug/app-debug.apk` |
| APK Size | 6.17 MB |
| Build Time | 47s |
| Package | `com.logisticapp.emuladortelnet` |
| Min API | 24 |
| Target API | 34 |
| Status | ✅ BUILD SUCCESS |

---

## 📝 Próximas Implementações (Após Testar)

Uma vez confirmado que a UI funciona:

### Fase 1: Real Telnet Protocol
```kotlin
socket = Socket(host, port.toInt())
val inputStream = socket.getInputStream()
val outputStream = socket.getOutputStream()
```

### Fase 2: Terminal Emulation
- Parse ANSI escape codes
- VT100 terminal support
- Color text rendering

### Fase 3: License System
- Device fingerprinting
- License validation
- Expiration handling

### Fase 4: Data Persistence
- Room database
- Connection history
- Settings storage

---

## 🎯 Próximo Passo

Escolha uma das 3 opções acima e teste o APK!

Após confirmar que funciona, posso ajudar com:
- Implementação de Telnet real
- Terminal ANSI/VT100
- Sistema de licença
- Persistência de dados

---

**Gerado**: May 21, 2026
**Status**: APK 100% pronto, aguardando teste
