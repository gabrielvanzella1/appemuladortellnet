# ========================================
# Script Automatizado: Setup Emulador Telnet
# ========================================
# Use: powershell.exe -File "setup-automatizado.ps1"
# ========================================

param(
    [string]$UserPath = "C:\Users\$env:USERNAME",
    [string]$ProjectPath = "$UserPath\web\trabalho\appEmuladorTellnet\EmuladorTelnet",
    [int]$Port = 23
)

$ErrorActionPreference = "Stop"

function Write-Section {
    param([string]$Title)
    Write-Host "`n" + "="*50 -ForegroundColor Cyan
    Write-Host "  $Title" -ForegroundColor Cyan
    Write-Host "="*50 -ForegroundColor Cyan
}

function Write-Step {
    param([string]$Message)
    Write-Host "▶ $Message" -ForegroundColor Yellow
}

function Write-Success {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor Green
}

function Write-Error-Custom {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor Red
}

# ========================================
# 1. Verificar Pré-requisitos
# ========================================
Write-Section "VERIFICANDO PRÉ-REQUISITOS"

Write-Step "Java 11..."
if (Get-Command java -ErrorAction SilentlyContinue) {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    if ($javaVersion -like "*11*") {
        Write-Success "Java 11 encontrado: $javaVersion"
        $env:JAVA_HOME = "C:\Program Files\Amazon Corretto\jdk11.0.31_11"
    } else {
        Write-Error-Custom "Java 11 não encontrado. Instale com: winget install Amazon.Corretto.11"
        exit 1
    }
} else {
    Write-Error-Custom "Java não está no PATH"
    exit 1
}

Write-Step "Git..."
if (Get-Command git -ErrorAction SilentlyContinue) {
    Write-Success "Git encontrado"
} else {
    Write-Error-Custom "Git não encontrado. Instale com: winget install Git.Git"
    exit 1
}

Write-Step "Projeto..."
if (Test-Path "$ProjectPath\app") {
    Write-Success "Projeto encontrado em: $ProjectPath"
} else {
    Write-Error-Custom "Projeto não encontrado em: $ProjectPath"
    exit 1
}

# ========================================
# 2. Verificar/Criar Android SDK
# ========================================
Write-Section "VERIFICANDO ANDROID SDK"

$sdkDir = "$env:LOCALAPPDATA\Android\Sdk"
$avdmanager = "$sdkDir\cmdline-tools\cmdline-tools\bin\avdmanager.bat"
$sdkmanager = "$sdkDir\cmdline-tools\cmdline-tools\bin\sdkmanager.bat"

if (Test-Path $sdkDir) {
    Write-Success "Android SDK encontrado: $sdkDir"
} else {
    Write-Error-Custom "Android SDK não encontrado!"
    Write-Host "Baixe de: https://developer.android.com/studio/command-line-tools" -ForegroundColor Magenta
    exit 1
}

# ========================================
# 3. Verificar local.properties
# ========================================
Write-Section "VERIFICANDO local.properties"

$localProps = "$ProjectPath\local.properties"
if (-not (Test-Path $localProps)) {
    Write-Step "Criando local.properties..."
    $content = "sdk.dir=$sdkDir".Replace("\", "\\")
    Set-Content -Path $localProps -Value $content
    Write-Success "local.properties criado"
} else {
    Write-Success "local.properties já existe"
}

# ========================================
# 4. Compilar Projeto
# ========================================
Write-Section "COMPILANDO PROJETO"

cd $ProjectPath

Write-Step "Limpando build anterior..."
.\gradlew.bat clean 2>&1 | Select-Object -Last 3

Write-Step "Compilando APK (isso pode levar 2-3 minutos)..."
$buildResult = .\gradlew.bat assembleDebug 2>&1

if ($buildResult -like "*BUILD SUCCESSFUL*") {
    Write-Success "APK compilado com sucesso!"
    
    $apkPath = "$ProjectPath\app\build\outputs\apk\debug\app-debug.apk"
    $apkSize = (Get-Item $apkPath).Length / 1MB
    Write-Host "Tamanho: $([math]::Round($apkSize, 2)) MB" -ForegroundColor Green
} else {
    Write-Error-Custom "Falha na compilação!"
    Write-Host $buildResult | Select-Object -Last 20
    exit 1
}

# ========================================
# 5. Criar AVD
# ========================================
Write-Section "CRIANDO ANDROID VIRTUAL DEVICE"

Write-Step "Verificando AVDs..."
$avdList = & $avdmanager list avd 2>&1 | Select-String "Name:"

if ($null -ne $avdList) {
    Write-Success "AVDs já existem"
} else {
    Write-Step "Criando novo AVD 'TestDevice'..."
    echo "" | & $avdmanager create avd -n "TestDevice" `
        -k "system-images;android-34;google_apis;x86_64" --force 2>&1 | Select-Object -Last 5
    Write-Success "AVD criado"
}

# ========================================
# 6. Iniciar Emulator
# ========================================
Write-Section "INICIANDO EMULATOR"

$emulator = "$sdkDir\emulator\emulator.exe"

if (Test-Path $emulator) {
    Write-Step "Iniciando emulator TestDevice..."
    Write-Host "Aguarde 30-60 segundos para iniciar..." -ForegroundColor Magenta
    
    # Iniciar em background
    Start-Process -FilePath $emulator -ArgumentList "-avd TestDevice -no-snapshot-load -no-audio"
    
    Write-Success "Emulator iniciado em background"
    Write-Step "Aguardando device conectar (60 seg)..."
    
    $adb = "$sdkDir\platform-tools\adb.exe"
    & $adb wait-for-device
    
    Write-Success "Device conectado!"
    & $adb devices
} else {
    Write-Error-Custom "Emulator não encontrado"
    exit 1
}

# ========================================
# 7. Instalar APK
# ========================================
Write-Section "INSTALANDO APK"

$adb = "$sdkDir\platform-tools\adb.exe"
$apkPath = "$ProjectPath\app\build\outputs\apk\debug\app-debug.apk"

Write-Step "Instalando app no emulator..."
$installResult = & $adb install -r $apkPath 2>&1

if ($installResult -like "*Success*") {
    Write-Success "APK instalado com sucesso!"
} else {
    Write-Error-Custom "Falha na instalação"
    Write-Host $installResult | Select-Object -Last 10
    exit 1
}

# ========================================
# 8. Iniciar Aplicação
# ========================================
Write-Section "INICIANDO APLICAÇÃO"

Write-Step "Abrindo MainActivity..."
& $adb shell am start -n "com.logisticapp.emuladortelnet/.MainActivity"

Write-Success "Aplicação iniciada!"

# ========================================
# 9. Verificar Logs
# ========================================
Write-Section "VERIFICANDO LOGS"

Write-Step "Aguardando 3 segundos para app inicializar..."
Start-Sleep -Seconds 3

$logs = & $adb logcat -d "AndroidRuntime:E" 2>&1
if ($logs -like "*Exception*") {
    Write-Error-Custom "Erros encontrados nos logs!"
    Write-Host $logs | Select-Object -Last 20
} else {
    Write-Success "Sem erros criticos!"
}

# ========================================
# 10. Conclusão
# ========================================
Write-Section "SETUP CONCLUÍDO!"

Write-Host @"

✓ Projeto compilado com sucesso
✓ Emulator rodando
✓ App instalado e aberto

PRÓXIMOS PASSOS:

1. Teste de conexão:
   - Host: towel.blinkenlights.nl
   - Porta: 23
   - Clique em CONECTAR

2. Ou use servidor local:
   - Host: 10.0.2.2 (para Windows host)
   - Porta: 23

3. Para mais informações:
   - Leia: GUIA_SETUP_OUTRO_PC.md
   - Pergunte ao Copilot qualquer coisa

DÚVIDAS?
Pergunte ao GitHub Copilot no VS Code!

"@ -ForegroundColor Green

Write-Host "Pressione ENTER para sair..." -ForegroundColor Yellow
Read-Host
