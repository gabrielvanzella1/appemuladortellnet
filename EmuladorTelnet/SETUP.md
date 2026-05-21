# ⚙️ Guia de Instalação e Setup - Emulador Telnet

## 🔴 Pré-requisitos Necessários

Para compilar e rodar o app Android, você precisa de:

### 1. Java Development Kit (JDK) 11+
**Status**: ❌ **NÃO ENCONTRADO**

#### Opção A: Instalar via Chocolatey (Recomendado)
```powershell
# Como Admin no PowerShell:
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
choco install openjdk11 -y
```

#### Opção B: Instalar Manualmente
1. Baixar: https://adoptopenjdk.net/ (OpenJDK 11 LTS)
2. Instalar normalmente
3. Verificar instalação:
```powershell
java -version
javac -version
```

#### Opção C: Via Windows Package Manager
```powershell
winget install Oracle.JDK.11
```

---

### 2. Android SDK
**Status**: ❌ **NÃO ENCONTRADO**

#### Opção A: Instalar Android Studio (RECOMENDADO)
1. Baixar: https://developer.android.com/studio
2. Instalar
3. Durante instalação, instalar:
   - Android SDK
   - Android SDK Platform 34
   - Android Emulator
   - Android SDK Command-line Tools

#### Opção B: Apenas SDK (sem IDE)
```powershell
# Via Chocolatey
choco install android-sdk -y
```

---

### 3. Gradle (Automático)
✅ **Já preparado** - O Gradle Wrapper (`gradlew.bat`) será baixado automaticamente

---

## 🚀 Setup Passo a Passo

### Passo 1: Instalar Java
```powershell
# Verificar se Java está instalado
java -version

# Deve retorgar algo como:
# openjdk version "11.0.x"
```

### Passo 2: Instalar Android SDK
```powershell
# Após instalar Android Studio:
# Abrir Android Studio → SDK Manager → Instalar:
# - Android SDK Platform 34
# - Build Tools 34.0.0
# - Platform Tools
# - Android Emulator
```

### Passo 3: Configurar Variáveis de Ambiente
```powershell
# Abrir PowerShell como Admin:

# 1. Definir JAVA_HOME
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-11", "User")

# 2. Definir ANDROID_HOME (se instalou Android Studio)
[Environment]::SetEnvironmentVariable("ANDROID_HOME", "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk", "User")

# 3. Adicionar ao PATH
$path = [Environment]::GetEnvironmentVariable("Path", "User")
$path += ";C:\Program Files\Java\jdk-11\bin"
$path += ";C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools"
$path += ";C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\tools"
[Environment]::SetEnvironmentVariable("Path", $path, "User")

# 4. REABRIR PowerShell para aplicar mudanças
exit
```

### Passo 4: Verificar Setup
```powershell
java -version
android list targets
```

---

## 📱 Compilar e Rodar o App

### Opção 1: No VS Code + Android Emulator

```powershell
cd C:\Users\7700924385\web\trabalho\appEmuladorTellnet\EmuladorTelnet

# 1. Compilar APK
.\gradlew.bat assembleDebug

# 2. Abrir emulador Android Studio

# 3. Instalar APK
adb install app\build\outputs\apk\debug\app-debug.apk

# 4. Abrir app
adb shell am start -n com.logisticapp.emuladortelnet/.MainActivity
```

### Opção 2: No Android Studio

```powershell
# Abrir o projeto no Android Studio:
Start-Process "C:\Program Files\Android\Android Studio\bin\studio64.exe" "C:\Users\7700924385\web\trabalho\appEmuladorTellnet\EmuladorTelnet"
```

---

## 🐛 Troubleshooting

### Erro: "JAVA_HOME is not set"
```powershell
# Verificar se Java está no PATH
Get-Command java

# Se retornar vazio:
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-11", "User")
$env:JAVA_HOME = "C:\Program Files\Java\jdk-11"
```

### Erro: "Android SDK not found"
```powershell
# Verificar se Android SDK está instalado
Test-Path "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk"

# Se false, instalar Android Studio primeiro
```

### Erro: "Gradle build failed"
```powershell
# Limpar cache
.\gradlew.bat clean

# Tentar novamente
.\gradlew.bat assembleDebug --stacktrace
```

---

## ✅ Checklist de Instalação

Quando tudo estiver pronto, execute:

```powershell
$checks = @{
    "Java" = (Get-Command java -ErrorAction SilentlyContinue) -ne $null
    "Gradle" = (Test-Path ".\gradlew.bat") -eq $true
    "Android SDK" = (Test-Path $env:ANDROID_HOME) -eq $true
}

$checks | ForEach-Object { Write-Host "$_ $(if($_.Value) {'✅'} else {'❌'})" }
```

---

## 📝 Resumo

| Componente | Status | Ação |
|-----------|--------|------|
| Java 11+ | ❌ | Instalar via Chocolatey ou manual |
| Android SDK | ❌ | Instalar Android Studio |
| Gradle Wrapper | ✅ | Já pronto |
| Projeto Base | ✅ | Pronto para compilar |

---

**PRÓXIMO PASSO**: Instale Java e Android SDK, depois me avise! Aí compilamos e rodamos o app. 🚀
