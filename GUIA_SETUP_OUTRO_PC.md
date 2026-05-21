# 📱 Guia Completo: Setup Emulador Telnet em Outro PC

> **Usando GitHub Copilot para auxiliar o setup**

---

## 🎯 Objetivo

Reproduzir o projeto **Emulador Telnet** em um novo PC do zero até ter a aplicação rodando no emulator.

**Tempo estimado:** 45-60 minutos (primeira vez)

---

## ✅ Pré-requisitos

- Windows 10/11
- ~50GB de espaço livre (Android SDK + emulator)
- Internet estável (downloads grandes)
- GitHub Copilot ativo no VS Code

---

## 📋 Parte 1: Preparar Ambiente

### 1.1 Instalar VS Code + Copilot

```bash
# Baixar VS Code em https://code.visualstudio.com
# Instalar GitHub Copilot via extensões
```

**No Copilot, pergunte:**
```
Ajuda a configurar GitHub Copilot e VS Code para desenvolvimento Android
```

### 1.2 Instalar Git

```powershell
# Via winget (recomendado)
winget install Git.Git

# Verificar instalação
git --version
```

### 1.3 Clonar/Copiar Projeto

```powershell
# Opção A: Clone do repositório
git clone <seu-repo>

# Opção B: Copiar pasta do outro PC
# Ou baixar ZIP do repositório
```

Coloque em: `C:\Users\<seu-usuario>\web\trabalho\appEmuladorTellnet\EmuladorTelnet`

---

## 📦 Parte 2: Instalar Ferramentas Necessárias

### 2.1 Instalar Java 11 (Obrigatório)

```powershell
# Via winget
winget install Amazon.Corretto.11

# Verificar
java -version
# Output: openjdk version "11.0.xx"
```

### 2.2 Instalar Java 17 (Opcional para futuro)

```powershell
winget install Amazon.Corretto.17
```

### 2.3 Criar local.properties

Abra o projeto no VS Code e crie arquivo `local.properties` na raiz:

```properties
sdk.dir=C:\\Users\\<seu-usuario>\\AppData\\Local\\Android\\Sdk
```

**Peça ao Copilot:**
```
Como configurar local.properties para Android SDK no Windows?
```

---

## 🚀 Parte 3: Instalar Android SDK

### 3.1 Download SDK Command-line Tools

1. Vá em: https://developer.android.com/studio/command-line-tools
2. Download: `commandlinetools-win-*.zip` (~141 MB)
3. Extrair em: `C:\Users\<seu-usuario>\AppData\Local\Android\`
4. Renomear pasta para: `cmdline-tools\cmdline-tools`

### 3.2 Aceitar Licenças

```powershell
$env:JAVA_HOME = "C:\Program Files\Amazon Corretto\jdk11.0.31_11"

# Criar diretório de licenses
New-Item -ItemType Directory -Force `
  "$env:LOCALAPPDATA\Android\Sdk\licenses"

# Adicionar hashes de licenças
$license = "24333f8a63b6825ea9c5514f83c2829b004d1fee"
Add-Content "$env:LOCALAPPDATA\Android\Sdk\licenses\android-sdk-license" $license
```

### 3.3 Instalar Componentes SDK

```powershell
$env:JAVA_HOME = "C:\Program Files\Amazon Corretto\jdk11.0.31_11"
$avdmanager = "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\cmdline-tools\bin\avdmanager.bat"
$sdkmanager = "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\cmdline-tools\bin\sdkmanager.bat"

Write-Host "Instalando componentes SDK..." -ForegroundColor Cyan

# Build tools
& $sdkmanager "build-tools;30.0.3"

# Platform
& $sdkmanager "platforms;android-34"

# System image
& $sdkmanager "system-images;android-34;google_apis;x86_64"

# Platform tools
& $sdkmanager "platform-tools"
```

**Peça ao Copilot se tiver dúvida:**
```
Como instalar Android SDK components via sdkmanager?
```

---

## 🔨 Parte 4: Compilar Projeto

### 4.1 Verificar Gradle

```powershell
cd "C:\Users\<seu-usuario>\web\trabalho\appEmuladorTellnet\EmuladorTelnet"

# Testar gradle
.\gradlew.bat --version
# Output: Gradle 8.6 with Java 11
```

### 4.2 Compilar APK

```powershell
$env:JAVA_HOME = "C:\Program Files\Amazon Corretto\jdk11.0.31_11"

cd "C:\Users\<seu-usuario>\web\trabalho\appEmuladorTellnet\EmuladorTelnet"

# Clean + build
.\gradlew.bat clean assembleDebug

# Aguarde ~2-3 minutos
# Sucesso: "BUILD SUCCESSFUL in XX seconds"
```

**Se der erro:**
```
Pergunte ao Copilot:
"Gradle build falhou com erro [ERRO_AQUI], como resolver?"
```

### 4.3 Verificar APK

```powershell
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"

# Deve ter ~6-7 MB
Get-Item $apkPath | Select-Object -Property Name, Length
```

---

## 📱 Parte 5: Criar e Iniciar Emulator

### 5.1 Criar AVD (Android Virtual Device)

```powershell
$env:JAVA_HOME = "C:\Program Files\Amazon Corretto\jdk11.0.31_11"
$avdmanager = "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\cmdline-tools\bin\avdmanager.bat"

Write-Host "Criando AVD..." -ForegroundColor Cyan

echo "" | & $avdmanager create avd -n "TestDevice" `
  -k "system-images;android-34;google_apis;x86_64" --force
```

### 5.2 Iniciar Emulator

```powershell
$env:JAVA_HOME = "C:\Program Files\Amazon Corretto\jdk17.0.19_10"
$emulator = "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe"

Write-Host "Iniciando emulator..." -ForegroundColor Green
Write-Host "Aguarde 30-60 segundos..." -ForegroundColor Yellow

& $emulator -avd TestDevice -no-snapshot-load -no-audio
```

**Deixe aberto em janela separada!**

### 5.3 Aguardar Device

Em outra janela PowerShell:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

Write-Host "Aguardando emulator conectar..." -ForegroundColor Yellow
& $adb wait-for-device

Write-Host "Device conectado!" -ForegroundColor Green
& $adb devices
```

---

## 📲 Parte 6: Instalar e Rodar App

### 6.1 Instalar APK

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$apkPath = "C:\Users\<seu-usuario>\web\trabalho\appEmuladorTellnet\EmuladorTelnet\app\build\outputs\apk\debug\app-debug.apk"

Write-Host "Instalando APK..." -ForegroundColor Cyan
& $adb install -r $apkPath

# Sucesso: "Success"
```

### 6.2 Iniciar Aplicação

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

Write-Host "Iniciando app..." -ForegroundColor Green
& $adb shell am start -n "com.logisticapp.emuladortelnet/.MainActivity"

Write-Host "App está abrindo no emulator!" -ForegroundColor Green
```

### 6.3 Verificar se Abriu

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

# Ver logs
& $adb logcat -d "AndroidRuntime:E" | Select-Object -Last 5

# Se vazio = sucesso!
```

---

## 🧪 Parte 7: Testar Conexão Telnet

### 7.1 Opção A: Servidor Público

No app:
```
Host: towel.blinkenlights.nl
Porta: 23
```

Clique em CONECTAR - deve mostrar Star Wars!

### 7.2 Opção B: Servidor Local

Em nova janela PowerShell:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

# Achar seu IP local
$ip = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object InterfaceAlias -like "*Wi-Fi*").IPAddress
Write-Host "Seu IP local: $ip"

# No app, use: Host=$ip, Porta=23
```

Crie servidor Telnet em PowerShell (arquivo `telnet-server.ps1`):

```powershell
$listener = New-Object System.Net.Sockets.TcpListener([System.Net.IPAddress]::Any, 23)
$listener.Start()
Write-Host "Servidor Telnet rodando na porta 23..." -ForegroundColor Green

while ($true) {
    $client = $listener.AcceptTcpClient()
    $stream = $client.GetStream()
    $writer = New-Object System.IO.StreamWriter($stream)
    
    $writer.WriteLine("Bem-vindo ao servidor Telnet!")
    $writer.Flush()
    
    $reader = New-Object System.IO.StreamReader($stream)
    $line = $reader.ReadLine()
    $writer.WriteLine("Você digitou: $line")
    $writer.Flush()
    
    $reader.Dispose()
    $writer.Dispose()
    $client.Close()
}
```

Execute:
```powershell
powershell.exe -File "telnet-server.ps1"
```

---

## 🆘 Troubleshooting

### Problema: "SDK location not found"
**Solução:** Criar `local.properties` com caminho correto

### Problema: "Unable to start the daemon process"
**Solução:** 
```powershell
# Java versão errada
$env:JAVA_HOME = "C:\Program Files\Amazon Corretto\jdk11.0.31_11"
```

### Problema: "Unknown AVD name [TestDevice]"
**Solução:**
```powershell
# Recriar AVD
echo "" | & $avdmanager create avd -n "TestDevice" `
  -k "system-images;android-34;google_apis;x86_64" --force
```

### Problema: "Cannot access system provider: 'settings'"
**Solução:** Aguardar mais tempo o emulator inicializar (60+ segundos)

### Problema: "NullPointerException: findViewById(R.id.scrollView)"
**Solução:** Certificar que layout XML tem ID correto:
```xml
<ScrollView
    android:id="@+id/scrollView"
    ...>
```

---

## 📚 Usando Copilot para Ajuda

Durante o processo, você pode perguntar:

```
"Como faço para [problema específico] em Android?"
"Gradle falhou em [erro específico], qual é a solução?"
"Como debugar app Android no emulator?"
"Qual é a estrutura correta do arquivo [arquivo]?"
```

---

## ✨ Checklist Final

- [ ] Java 11 instalado
- [ ] Android SDK instalado
- [ ] local.properties criado
- [ ] APK compilado com sucesso
- [ ] AVD criado
- [ ] Emulator rodando
- [ ] App instalado
- [ ] App abrindo sem erros
- [ ] Conexão Telnet testada

---

## 🎯 Próximos Passos

Após confirmar que tudo está rodando:

1. **Implementar Telnet Real** (TCP Socket)
2. **Parser ANSI/VT100**
3. **Sistema de Licença**
4. **Database (Room)**

Cada um desses pode ser novo projeto/tarefa!

---

## 📞 Suporte

Se der problema em qualquer etapa:
1. Note o **erro exato**
2. Copie para o **Copilot Chat**
3. Copilot vai ajudar a resolver

**Comandos úteis para Copilot:**
```
"Estou recebendo este erro: [ERRO], como resolver?"
"Qual é o próximo passo na configuração de Android SDK?"
"Como debugar [problema] no VS Code?"
```

---

**Versão:** 1.0  
**Data:** 21/05/2026  
**Status:** ✅ Testado e funcional

