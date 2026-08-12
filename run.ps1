# ============================================================
#  TellX - Build e execucao no emulador Android
#  Uso: .\run.ps1                        (build sandbox + instala + abre)
#       .\run.ps1 -Flavor production     (build produção + instala + abre)
#       .\run.ps1 -SkipBuild             (instala + abre, sem compilar)
#       .\run.ps1 -SkipBuild -SkipInstall (so abre o app)
# ============================================================

param(
    [ValidateSet("sandbox", "production")]
    [string]$Flavor = "sandbox",
    [switch]$SkipBuild,
    [switch]$SkipInstall
)

$FLAVOR_CAP  = $Flavor.Substring(0,1).ToUpper() + $Flavor.Substring(1)
$NAMESPACE   = "com.logisticapp.emuladortelnet"
$PROJECT_DIR = "$PSScriptRoot\EmuladorTelnet"
$APK_PATH    = "$PROJECT_DIR\app\build\outputs\apk\$Flavor\debug\app-$Flavor-debug.apk"
$PACKAGE     = if ($Flavor -eq "sandbox") { "$NAMESPACE.sandbox" } else { $NAMESPACE }
$ACTIVITY    = "$PACKAGE/$NAMESPACE.LicenseActivity"
$AVD_NAME    = "Pixel_7"
$SDK         = "$env:LOCALAPPDATA\Android\Sdk"
$ADB         = "$SDK\platform-tools\adb.exe"
$EMULATOR    = "$SDK\emulator\emulator.exe"

function Write-Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Write-Ok($msg)   { Write-Host "    [OK] $msg" -ForegroundColor Green }
function Write-Fail($msg) { Write-Host "    [ERRO] $msg" -ForegroundColor Red; exit 1 }

# ------------------------------------------------------------------
# 1. Detectar dispositivo (prioriza celular fisico via USB)
# ------------------------------------------------------------------
Write-Step "Procurando dispositivo Android..."

function Get-OnlineDevices { (& $ADB devices) -split "`r?`n" | Where-Object { $_ -match "\tdevice$" } | ForEach-Object { ($_ -split "`t")[0] } }

$SERIAL  = $null
$online  = Get-OnlineDevices
$phys    = $online | Where-Object { $_ -notmatch "^emulator-" }

if ($phys) {
    $SERIAL = @($phys)[0]
    Write-Ok "Celular fisico conectado: $SERIAL"
} elseif ($online) {
    $SERIAL = @($online)[0]
    Write-Ok "Emulador ja esta rodando: $SERIAL"
} else {
    Write-Host "    Nenhum dispositivo encontrado." -ForegroundColor Yellow
    Write-Host "    Conecte o celular via USB com 'Depuracao USB' ligada e autorize este PC na tela do aparelho." -ForegroundColor Yellow
    Write-Host "    (Para usar o emulador: & '$EMULATOR' -avd $AVD_NAME -gpu host)" -ForegroundColor DarkGray
    Write-Host "    Aguardando dispositivo USB..." -NoNewline
    $waited = 0
    while ($waited -lt 120) {
        Start-Sleep -Seconds 2
        $waited += 2
        Write-Host "." -NoNewline
        $phys = Get-OnlineDevices | Where-Object { $_ -notmatch "^emulator-" }
        if ($phys) { $SERIAL = @($phys)[0]; break }
    }
    Write-Host ""
    if (-not $SERIAL) { Write-Fail "Nenhum celular detectado. Verifique cabo, 'Depuracao USB' e a autorizacao na tela do aparelho." }
    Write-Ok "Celular conectado: $SERIAL"
}

# ------------------------------------------------------------------
# 2. Build
# ------------------------------------------------------------------
if (-not $SkipBuild) {
    Write-Step "Compilando projeto (assemble${FLAVOR_CAP}Debug)..."
    Push-Location $PROJECT_DIR
    & ".\gradlew.bat" "assemble${FLAVOR_CAP}Debug" --no-daemon
    $code = $LASTEXITCODE
    Pop-Location
    if ($code -ne 0) { Write-Fail "Build falhou. Verifique os erros acima." }
    Write-Ok "Build concluido ($Flavor): $APK_PATH"
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
    $install = (& $ADB -s $SERIAL install -r $APK_PATH 2>&1) -join " "
    if ($install -notmatch "Success") { Write-Fail "Falha na instalacao: $install" }
    Write-Ok "APK instalado com sucesso."
} else {
    Write-Ok "Instalacao ignorada (-SkipInstall)."
}

# ------------------------------------------------------------------
# 4. Abrir app
# ------------------------------------------------------------------
Write-Step "Abrindo TellX ($Flavor) no dispositivo..."
& $ADB -s $SERIAL shell am start -n $ACTIVITY | Out-Null
Write-Ok "App iniciado!"
Write-Host ""
Write-Host "  TellX ($Flavor) rodando. Bom desenvolvimento!" -ForegroundColor Yellow
Write-Host ""
