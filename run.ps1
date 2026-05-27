# ============================================================
#  TellX - Build e execucao no emulador Android
#  Uso: .\run.ps1                        (build + instala + abre)
#       .\run.ps1 -SkipBuild             (instala + abre, sem compilar)
#       .\run.ps1 -SkipBuild -SkipInstall (so abre o app)
# ============================================================

param(
    [switch]$SkipBuild,
    [switch]$SkipInstall
)

$PROJECT_DIR = "$PSScriptRoot\EmuladorTelnet"
$APK_PATH    = "$PROJECT_DIR\app\build\outputs\apk\debug\app-debug.apk"
$PACKAGE     = "com.logisticapp.emuladortelnet"
$ACTIVITY    = "$PACKAGE/.LicenseActivity"
$AVD_NAME    = "TestDevice"
$SDK         = "$env:LOCALAPPDATA\Android\Sdk"
$ADB         = "$SDK\platform-tools\adb.exe"
$EMULATOR    = "$SDK\emulator\emulator.exe"

function Write-Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Write-Ok($msg)   { Write-Host "    [OK] $msg" -ForegroundColor Green }
function Write-Fail($msg) { Write-Host "    [ERRO] $msg" -ForegroundColor Red; exit 1 }

# ------------------------------------------------------------------
# 1. Verificar emulador
# ------------------------------------------------------------------
Write-Step "Verificando emulador..."
$devices = & $ADB devices 2>$null | Select-String "emulator"
if ($devices) {
    Write-Ok "Emulador ja esta rodando."
} else {
    Write-Step "Iniciando emulador '$AVD_NAME'..."
    Start-Process -FilePath $EMULATOR -ArgumentList "-avd $AVD_NAME -no-snapshot-load" -WindowStyle Normal

    Write-Host "    Aguardando boot" -NoNewline
    $timeout = 120
    $elapsed = 0
    while ($elapsed -lt $timeout) {
        Start-Sleep -Seconds 3
        $elapsed += 3
        $booted = & $ADB shell getprop sys.boot_completed 2>$null
        Write-Host "." -NoNewline
        if ($booted -match "1") { break }
    }
    Write-Host ""
    if ($elapsed -ge $timeout) { Write-Fail "Timeout aguardando emulador." }
    Write-Ok "Emulador pronto!"
}

# ------------------------------------------------------------------
# 2. Build
# ------------------------------------------------------------------
if (-not $SkipBuild) {
    Write-Step "Compilando projeto (assembleDebug)..."
    Push-Location $PROJECT_DIR
    & ".\gradlew.bat" assembleDebug --no-daemon
    $code = $LASTEXITCODE
    Pop-Location
    if ($code -ne 0) { Write-Fail "Build falhou. Verifique os erros acima." }
    Write-Ok "Build concluido: $APK_PATH"
} else {
    Write-Ok "Build ignorado (-SkipBuild)."
}

# ------------------------------------------------------------------
# 3. Instalar APK
# ------------------------------------------------------------------
if (-not $SkipInstall) {
    Write-Step "Instalando APK no emulador..."
    if (-not (Test-Path $APK_PATH)) {
        Write-Fail "APK nao encontrado: $APK_PATH -- rode sem -SkipBuild primeiro."
    }
    $install = (& $ADB install -r $APK_PATH 2>&1) -join " "
    if ($install -notmatch "Success") { Write-Fail "Falha na instalacao: $install" }
    Write-Ok "APK instalado com sucesso."
} else {
    Write-Ok "Instalacao ignorada (-SkipInstall)."
}

# ------------------------------------------------------------------
# 4. Abrir app
# ------------------------------------------------------------------
Write-Step "Abrindo TellX no emulador..."
& $ADB shell am start -n $ACTIVITY | Out-Null
Write-Ok "App iniciado!"
Write-Host ""
Write-Host "  TellX rodando. Bom desenvolvimento!" -ForegroundColor Yellow
Write-Host ""
