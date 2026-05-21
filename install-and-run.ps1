#!/bin/pwsh
$ProjectDir = "C:\Users\7700924385\web\trabalho\appEmuladorTellnet\EmuladorTelnet"
$ApkPath = "$ProjectDir\app\build\outputs\apk\debug\app-debug.apk"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$emulator = "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe"

function Wait-Device {
    Write-Host "Aguardando device/emulator conectar..." -ForegroundColor Yellow
    $timeout = 0
    while ($timeout -lt 120) {
        $devices = & $adb devices | Select-String "emulator|device"
        if ($devices -and -not ($devices -match "^List")) {
            Write-Host "[OK] Device conectado!" -ForegroundColor Green
            return $true
        }
        Start-Sleep -Seconds 2
        $timeout += 2
        Write-Host "  Tentativa $([math]::Round($timeout/2))..." -ForegroundColor Gray
    }
    Write-Host "[ERRO] Device nao respondeu" -ForegroundColor Red
    return $false
}

function Test-Emulator-Running {
    $emulators = Get-Process "qemu-system-x86_64" -ErrorAction SilentlyContinue
    if ($emulators) {
        Write-Host "[OK] Emulator ja esta rodando" -ForegroundColor Green
        return $true
    }
    return $false
}

if (-not (Test-Path $ApkPath)) {
    Write-Host "[ERRO] APK nao encontrado: $ApkPath" -ForegroundColor Red
    exit 1
}

Write-Host "`n========================================"
Write-Host "Instalador - Emulador Telnet"
Write-Host "========================================"

if (-not (Test-Emulator-Running)) {
    Write-Host "`n[INIT] Iniciando emulator..." -ForegroundColor Yellow
    if (-not (Test-Path $emulator)) {
        Write-Host "[ERRO] Emulator nao encontrado" -ForegroundColor Red
        exit 1
    }
    Start-Process $emulator -ArgumentList "-avd TelnetApp -no-snapshot-load -no-audio"
    Start-Sleep -Seconds 5
}

if (-not (Wait-Device)) {
    Write-Host "[ERRO] Falha ao conectar" -ForegroundColor Red
    exit 1
}

Write-Host "`n[CLEAN] Limpando versao anterior..." -ForegroundColor Yellow
& $adb uninstall "com.logisticapp.emuladortelnet" 2>&1 | Out-Null

Write-Host "`n[INSTALL] Instalando APK..." -ForegroundColor Cyan
$installOutput = & $adb install $ApkPath 2>&1

if ($LASTEXITCODE -eq 0 -and $installOutput -match "Success") {
    Write-Host "[OK] APK instalado com sucesso!" -ForegroundColor Green
    Write-Host "`n[LAUNCH] Iniciando aplicacao..." -ForegroundColor Yellow
    & $adb shell am start -n "com.logisticapp.emuladortelnet/.MainActivity"
    Write-Host "`n[SUCCESS] Aplicacao iniciada!" -ForegroundColor Green
} else {
    Write-Host "[ERRO] Falha na instalacao" -ForegroundColor Red
    exit 1
}
