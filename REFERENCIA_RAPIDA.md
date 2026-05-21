# 🚀 Referência Rápida: Setup Emulador Telnet

## ⚡ Instalação Rápida (Copy-Paste)

### 1️⃣ Instalar Java 11
```powershell
winget install Amazon.Corretto.11
```

### 2️⃣ Criar local.properties
```powershell
# No diretório do projeto
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
Add-Content "local.properties" "sdk.dir=$sdk".Replace("\", "\\")
```

### 3️⃣ Compilar APK
```powershell
$env:JAVA_HOME = "C:\Program Files\Amazon Corretto\jdk11.0.31_11"
cd "seu\caminho\EmuladorTelnet"
.\gradlew.bat clean assembleDebug
```

### 4️⃣ Criar AVD
```powershell
$env:JAVA_HOME = "C:\Program Files\Amazon Corretto\jdk11.0.31_11"
$avdmanager = "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\cmdline-tools\bin\avdmanager.bat"
echo "" | & $avdmanager create avd -n "TestDevice" -k "system-images;android-34;google_apis;x86_64" --force
```

### 5️⃣ Iniciar Emulator
```powershell
$env:JAVA_HOME = "C:\Program Files\Amazon Corretto\jdk17.0.19_10"
$emulator = "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe"
& $emulator -avd TestDevice -no-snapshot-load -no-audio
```

### 6️⃣ Instalar + Rodar App
```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$apk = "seu\caminho\app\build\outputs\apk\debug\app-debug.apk"

& $adb wait-for-device
& $adb install -r $apk
& $adb shell am start -n "com.logisticapp.emuladortelnet/.MainActivity"
```

---

## 🔧 Variáveis de Ambiente (Salvar em Perfil PowerShell)

Adicione ao seu `$PROFILE`:

```powershell
# Android
$env:JAVA_HOME = "C:\Program Files\Amazon Corretto\jdk11.0.31_11"
$env:ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\Android\Sdk"

# Aliases úteis
New-Alias -Name gradlew -Value "$PWD\gradlew.bat" -Force
```

Para ativar:
```powershell
# Achar arquivo
$PROFILE

# Editar
notepad $PROFILE

# Recarregar
. $PROFILE
```

---

## 📱 Comandos ADB Úteis

```powershell
# Lista devices
adb devices

# Instalar APK
adb install app.apk

# Desinstalar app
adb uninstall com.logisticapp.emuladortelnet

# Ver logs
adb logcat

# Ver logs com filtro
adb logcat "AndroidRuntime:E"

# Limpar logs
adb logcat -c

# Iniciar activity
adb shell am start -n "package/activity"

# Parar app
adb shell am force-stop package

# Espiar arquivos
adb shell ls /data/data/package/

# Puxar arquivo
adb pull /data/data/package/file.db

# Executar comando shell
adb shell getprop ro.runtime.firstboot
```

---

## 🧪 Testes de Conexão

### Público (Telnet Star Wars)
```
Host: towel.blinkenlights.nl
Porta: 23
```

### Local (seu IP)
```
Achar IP: ipconfig /all
Host: <seu-ip-local>
Porta: 23
```

### Emulator → Host
```
Host: 10.0.2.2
Porta: 23
```

---

## 🔍 Troubleshooting Rápido

| Erro | Solução |
|------|---------|
| `Unknown AVD name` | Recriar AVD com `avdmanager create` |
| `SDK location not found` | Criar/verificar `local.properties` |
| `Cannot access system provider` | Aguardar +30 seg para emulator inicializar |
| `NullPointerException: findViewById` | Verificar ID do elemento no XML layout |
| `JAVA_HOME not set` | `$env:JAVA_HOME = "C:\...jdk11..."` |
| `Port already in use` | Emulator está rodando de outra vez |

---

## 📁 Estrutura de Pastas

```
EmuladorTelnet/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/logisticapp/emuladortelnet/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── ui/TelnetViewModel.kt
│   │   │   │   └── data/Models.kt
│   │   │   ├── res/
│   │   │   │   ├── layout/activity_main.xml
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   └── styles.xml
│   │   │   │   └── mipmap-*/ic_launcher.png
│   │   │   └── AndroidManifest.xml
│   │   └── test/...
│   ├── build.gradle.kts
│   └── build/outputs/apk/debug/app-debug.apk ← APK compilado
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       └── gradle-wrapper.properties
├── local.properties ← Criar aqui
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew.bat
└── gradlew
```

---

## 🤖 Como Usar Copilot

No VS Code, abra **Copilot Chat** (Ctrl+Shift+I) e pergunte:

```
Tenho um erro de build [ERRO_AQUI], como resolver?
```

```
Como debugar app Android no emulator?
```

```
Como implementar [feature] em Kotlin Android?
```

```
O arquivo [arquivo] está correto? Verifique.
```

---

## 📞 Checklist para Novo PC

- [ ] Java 11 instalado
- [ ] Git instalado
- [ ] Projeto clonado/copiado
- [ ] Android SDK instalado
- [ ] local.properties criado
- [ ] SDK components instalados (build-tools, platform, system-image)
- [ ] Licenças aceitas
- [ ] APK compilado
- [ ] AVD criado
- [ ] Emulator iniciado
- [ ] App instalado
- [ ] App funcionando
- [ ] Conexão Telnet testada

---

## 📚 Arquivos de Referência

- `GUIA_SETUP_OUTRO_PC.md` - Guia completo passo-a-passo
- `setup-automatizado.ps1` - Script que faz tudo automaticamente
- `REFERENCIA_RAPIDA.md` - Este arquivo

---

**Versão:** 1.0  
**Atualizado:** 21/05/2026

