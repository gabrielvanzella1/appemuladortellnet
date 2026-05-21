# 🚀 Testando o Emulador Telnet

## ✅ APK Gerado com Sucesso!

- **Arquivo**: `app/build/outputs/apk/debug/app-debug.apk`
- **Tamanho**: 6.17 MB
- **Package**: `com.logisticapp.emuladortelnet`

---

## 📱 Opções de Teste

### **OPÇÃO 1: Usando Script Automático** (Recomendado)

```powershell
cd "C:\Users\7700924385\web\trabalho\appEmuladorTellnet"
.\install-and-run.ps1
```

Este script irá:
1. Iniciar emulator (ou usar um já aberto)
2. Aguardar device conectar
3. Instalar APK
4. Abrir aplicação

---

### **OPÇÃO 2: Passo a Passo Manual**

#### 1️⃣ Iniciar Emulator
```powershell
$env:JAVA_HOME = "C:\Program Files\Amazon Corretto\jdk17.0.19_10"
$emulator = "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe"
& $emulator -avd TelnetApp -no-snapshot-load -no-audio
```

#### 2️⃣ Aguardar conexão
```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices
```

Aguarde aparecer algo como:
```
emulator-5554   device
```

#### 3️⃣ Instalar APK
```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install "C:\Users\7700924385\web\trabalho\appEmuladorTellnet\EmuladorTelnet\app\build\outputs\apk\debug\app-debug.apk"
```

#### 4️⃣ Abrir Aplicação
```powershell
& $adb shell am start -n "com.logisticapp.emuladortelnet/.MainActivity"
```

---

### **OPÇÃO 3: Usar Android Studio**

1. Abrir Android Studio
2. File → Open → Selecionar pasta `EmuladorTelnet`
3. AVD Manager → Criar/Iniciar emulator
4. Run → Run 'app'

---

### **OPÇÃO 4: Enviar para Device Físico**

1. Conectar device via USB
2. Ativar "USB Debugging" nas opções do desenvolvedor
3. Executar:
```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install "app\build\outputs\apk\debug\app-debug.apk"
```

---

## 🎯 O que Você Vai Ver

### Tela Inicial
```
┌─────────────────────────────┐
│  EMULADOR TELNET            │
├─────────────────────────────┤
│  Host: [_________________]  │
│  Port: [_____] (23)         │
│                             │
│  [Conectar] [Desconectar]   │
├─────────────────────────────┤
│                             │
│  Terminal Output Area       │
│  (preto com texto verde)    │
│                             │
│  Status: Desconectado       │
└─────────────────────────────┘
```

### Funcionalidades Atuais
- ✅ Input de Host/IP
- ✅ Input de Porta (padrão 23)
- ✅ Botões Conectar/Desconectar
- ✅ Terminal Display
- ✅ Status Text
- ⏳ Conexão Telnet: Ainda placeholder (simula conexão)

### Funcionalidades Futuras
- ❌ Telnet real (TCP socket)
- ❌ TELNET protocol (RFC 854)
- ❌ Terminal ANSI/VT100
- ❌ License system
- ❌ Connection history

---

## 🐛 Troubleshooting

### "Device not found"
```powershell
# Verificar status
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices

# Reiniciar adb
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" kill-server
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" start-server
```

### "Emulator fails to start"
- Verificar se Hyper-V está habilitado: `Get-WindowsOptionalFeature -Online -FeatureName Hyper-V`
- Ou usar `-accel off` ao iniciar: `emulator -avd TelnetApp -accel off`

### "APK installation fails"
```powershell
# Desinstalar versão anterior
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" uninstall "com.logisticapp.emuladortelnet"

# Tentar instalar novamente
```

### "JAVA_HOME not set"
```powershell
$env:JAVA_HOME = "C:\Program Files\Amazon Corretto\jdk17.0.19_10"
```

---

## 📊 Requisitos de Hardware

| Requisito | Mínimo | Recomendado |
|-----------|--------|------------|
| RAM | 2 GB | 4+ GB |
| CPU | 2 cores | 4+ cores |
| Armazenamento | 3 GB | 10 GB |
| GPU | Não | Suportada (aceleração) |

---

## 📝 Próximas Etapas

Uma vez testado, implementar:

1. **Telnet Real** (`IMPLEMENTATION_GUIDE.md`)
   ```kotlin
   // Implementar TCP socket connection
   socket = Socket(host, port.toInt())
   ```

2. **Terminal Emulation**
   ```kotlin
   // Parse ANSI escape codes
   // Handle VT100 sequences
   ```

3. **License System**
   ```kotlin
   // Device fingerprinting
   // License validation
   ```

4. **Data Persistence**
   ```kotlin
   // Room database
   // Connection history
   ```

---

## 🎓 Estrutura do Projeto

```
EmuladorTelnet/
├── app/
│   ├── src/main/
│   │   ├── kotlin/
│   │   │   ├── MainActivity.kt          ← UI Controller
│   │   │   ├── ui/TelnetViewModel.kt    ← State Management
│   │   │   └── data/Models.kt           ← Data Classes
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml ← UI Layout
│   │   │   ├── values/                  ← Resources
│   │   │   └── mipmap/                  ← Icons
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts                     ← Root config
├── gradle/
│   ├── libs.versions.toml               ← Dependency versions
│   └── wrapper/                         ← Gradle wrapper
├── local.properties                     ← SDK paths
├── install-and-run.ps1                  ← Script de instalação
└── app-debug.apk                        ← APK compilado ✓

```

---

## 📞 Suporte

**Erros comuns**:
- SDK não instalado → instale com setup-android-sdk.ps1
- Java path não encontrado → configure JAVA_HOME
- Emulator não inicia → ative Hyper-V ou use -accel off
- APK não instala → desinstale versão anterior primeiro

**Documentação**:
- `PROJECT_STATUS.md` - Status técnico
- `IMPLEMENTATION_GUIDE.md` - Próximas implementações
- `README_COMPILACAO.md` - Build details

---

**Status**: ✅ APK pronto para teste
**Data**: May 21, 2026
**Versão App**: 1.0-debug
