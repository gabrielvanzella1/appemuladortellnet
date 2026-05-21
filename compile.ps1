#!/bin/pwsh
# Script de compilação final com retry

$ProjectDir = "C:\Users\7700924385\web\trabalho\appEmuladorTellnet\EmuladorTelnet"
$AndroidHome = "$env:LOCALAPPDATA\Android\Sdk"

function Test-SDK {
    $files = @(
        "$AndroidHome\platforms\android-34\android.jar",
        "$AndroidHome\build-tools\34.0.0\dx.jar",
        "$AndroidHome\platform-tools\adb.exe"
    )
    
    foreach ($file in $files) {
        if (-not (Test-Path $file)) {
            Write-Host "❌ Faltando: $file" -ForegroundColor Red
            return $false
        }
    }
    Write-Host "✅ SDK instalado corretamente" -ForegroundColor Green
    return $true
}

function Compile {
    cd $ProjectDir
    
    Write-Host "`n📦 Iniciando compilação..." -ForegroundColor Cyan
    
    # Tentar com Java 17
    if (Test-Path "C:\Program Files\Amazon Corretto\jdk17.0.19_11\bin\java.exe") {
        Write-Host "☕ Usando Java 17" -ForegroundColor Yellow
        $env:JAVA_HOME = "C:\Program Files\Amazon Corretto\jdk17.0.19_11"
    } else {
        Write-Host "☕ Usando Java 11" -ForegroundColor Yellow
        $env:JAVA_HOME = "C:\Program Files\Amazon Corretto\jdk11.0.31_11"
    }
    
    $env:ANDROID_HOME = $AndroidHome
    
    Write-Host "JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Gray
    Write-Host "ANDROID_HOME: $env:ANDROID_HOME" -ForegroundColor Gray
    
    # Clean build
    .\gradlew.bat clean assembleDebug
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "`n✅ Compilação bem-sucedida!" -ForegroundColor Green
        Write-Host "`n📱 APK gerado em:" -ForegroundColor Cyan
        Write-Host "app/build/outputs/apk/debug/app-debug.apk" -ForegroundColor White
        return $true
    } else {
        Write-Host "`n❌ Compilação falhou" -ForegroundColor Red
        return $false
    }
}

# Main
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Compilador Android - Emulador Telnet" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if (Test-SDK) {
    Compile
} else {
    Write-Host "`n❌ SDK não está completo. Execute setup-android-sdk.ps1 primeiro" -ForegroundColor Red
    exit 1
}
