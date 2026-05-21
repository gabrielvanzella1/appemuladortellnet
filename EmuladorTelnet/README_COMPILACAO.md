# Emulador Telnet - Android App

Aplicação Android para emular conexões Telnet com suporte a licença para dispositivos de logística.

## 📋 Status

- ✅ Estrutura do projeto criada
- ✅ Build system Gradle configurado (Gradle 8.6 + AGP 7.4.2)
- ✅ Java 11 instalado e funcionando
- ✅ UI base implementada (MainActivity.kt)
- ✅ MVVM architecture com ViewModel
- ✅ Resource files (strings PT-BR, colors, themes)
- 🔄 Android SDK instalando em background
- 🔄 Java 17 instalando em background
- ❌ APK ainda não compilado
- ❌ Telnet protocol não implementado

## 🚀 Como Continuar

### 1. Aguardar Instalações (5-10 minutos)

Duas instalações estão em progresso em background:
- **Java 17**: Será usada para Gradle 9.5.1 (melhor suporte)
- **Android SDK**: Necessário para compilação do APK

Terminal de Java 17: `258c226c-b42c-4a6d-94e7-80c0090277f8`
Terminal de SDK: `6c65cfe5-dc76-4ac2-9699-d06a73433aaf`

### 2. Executar Setup do SDK (quando download terminar)

Uma vez que Java 17 + SDK estejam instalados, execute:

```powershell
cd C:\Users\7700924385\web\trabalho\appEmuladorTellnet
.\setup-android-sdk.ps1
```

Este script irá:
- Extrair Android SDK Command-line Tools
- Instalar SDK components (API 34, 24, build-tools)
- Aceitar licenças automaticamente
- Configurar environment variables

### 3. Compilar APK

```powershell
cd C:\Users\7700924385\web\trabalho\appEmuladorTellnet\EmuladorTelnet
$env:JAVA_HOME = "C:\Program Files\Amazon Corretto\jdk17.0.19_11"  # ou jdk11
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat clean assembleDebug
```

Ou use o script pronto:

```powershell
.\compile.ps1
```

### 4. Testar APK

APK será gerado em:
```
app/build/outputs/apk/debug/app-debug.apk
```

Para instalar em emulator/device:
```powershell
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.logisticapp.emuladortelnet/.MainActivity
```

## 📁 Estrutura do Projeto

```
EmuladorTelnet/
├── app/
│   ├── src/main/
│   │   ├── kotlin/com/logisticapp/emuladortelnet/
│   │   │   ├── MainActivity.kt
│   │   │   ├── ui/TelnetViewModel.kt
│   │   │   └── data/Models.kt
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml
│   │   │   ├── values/{strings,colors,styles}.xml
│   │   │   └── values-night/styles.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── gradle/libs.versions.toml
├── gradle/wrapper/
├── local.properties
├── compile.ps1
├── setup-android-sdk.ps1
├── PROJECT_STATUS.md
└── IMPLEMENTATION_GUIDE.md
```

## 🛠️ Stack Técnico

- **Language**: Kotlin 1.8.10
- **Android API**: 24 (min) - 34 (target)
- **Build System**: Gradle 8.6
- **Architecture**: MVVM + Repository Pattern
- **UI Framework**: AndroidX with Material Design
- **Async**: Coroutines

## 🔌 Dependências Principais

- `androidx.lifecycle:lifecycle-viewmodel-ktx` - MVVM
- `org.jetbrains.kotlinx:kotlinx-coroutines-android` - Async
- `com.squareup.okhttp3:okhttp` - HTTP/Networking
- `com.google.code.gson:gson` - JSON
- `com.jakewharton.timber:timber` - Logging

## 📱 UI Components

### Tela Principal (MainActivity)
- **HostInput**: Campo para IP/hostname
- **PortInput**: Campo para porta (default 23)
- **ConnectButton** / **DisconnectButton**: Controle de conexão
- **TerminalOutput**: Área de terminal (monospace, black bg, green text)
- **StatusText**: Indicador de estado de conexão

### Cores
- Background: #000000 (preto)
- Text: #00FF00 (verde)
- Error: #FF0000 (vermelho)
- Success: #00FF00 (verde)
- Info: #00FFFF (ciano)

## 🎯 Próximas Etapas

Após compilar com sucesso:

1. **Implementar Telnet Real** (IMPLEMENTATION_GUIDE.md)
   - Socket TCP connection
   - TELNET protocol negotiation (RFC 854)
   - IAC (Interpret As Command) handling
   - Command sending/receiving

2. **Terminal Emulation**
   - ANSI/VT100 escape sequence parsing
   - Cursor positioning
   - Text attributes

3. **License System**
   - Device fingerprinting
   - License validation
   - Expiration handling

4. **Data Persistence**
   - Room database for connection history
   - Settings storage

## 📋 Checklist de Build

- [ ] Java 17 instalado
- [ ] Android SDK instalado
- [ ] `local.properties` configurado
- [ ] `ANDROID_HOME` environment variable definido
- [ ] APK compilado com sucesso
- [ ] APK instalado em device/emulator
- [ ] App inicia corretamente
- [ ] UI renderiza corretamente
- [ ] Botões responsivos

## 🐛 Troubleshooting

### Build falha com "SDK location not found"
```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
```

### "Gradle requires JVM 17 or later"
```powershell
$env:JAVA_HOME = "C:\Program Files\Amazon Corretto\jdk17.0.19_11"
```

### Build do Kotlin lento
- Primeira compilação é sempre lenta
- Daemon do Gradle melhora builds subsequentes
- Clear `~\.gradle\caches` se tiver problemas

### Emulator não encontra APK
```powershell
# Verificar caminho
Get-ChildItem app/build/outputs/apk/debug/
# Reinstalar
adb uninstall com.logisticapp.emuladortelnet
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📖 Documentação Adicional

- `PROJECT_STATUS.md` - Status detalhado do projeto
- `IMPLEMENTATION_GUIDE.md` - Guia de implementação Telnet
- `README.md` (este arquivo) - Guia rápido

## 📞 Suporte

Erros comuns e soluções estão em `PROJECT_STATUS.md`.

---

**Last Updated**: Build preparation - aguardando SDK/Java 17
**Next Immediate**: Execute `setup-android-sdk.ps1` e `compile.ps1`
