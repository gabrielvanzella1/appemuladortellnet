# Script para setup Android SDK após Java 17 instalado

$AndroidHome = "$env:LOCALAPPDATA\Android\Sdk"
$CmdToolsZip = "$env:TEMP\cmdtools.zip"
$CmdToolsExtract = "$env:TEMP\cmdtools"

Write-Host "=== Android SDK Setup ===" -ForegroundColor Cyan

# 1. Esperar download completar
Write-Host "Aguardando download do SDK Command-line Tools..." -ForegroundColor Yellow
while ((Get-ChildItem $CmdToolsZip -ErrorAction SilentlyContinue).Length -eq 0) {
    Start-Sleep -Seconds 5
}
while ((Get-Process -Name msiexec -ErrorAction SilentlyContinue).Count -gt 0) {
    Write-Host "  Esperando instalação de Java 17..." -ForegroundColor Yellow
    Start-Sleep -Seconds 5
}

# 2. Extrair cmdlinetools
Write-Host "Extraindo Command-line Tools..." -ForegroundColor Cyan
Expand-Archive -Path $CmdToolsZip -DestinationPath $CmdToolsExtract -Force
$CmdToolsBin = Get-ChildItem -Path $CmdToolsExtract -Directory | Select-Object -First 1
Move-Item -Path "$($CmdToolsBin.FullName)\*" -Destination "$AndroidHome\cmdline-tools\latest\" -Force -ErrorAction SilentlyContinue

# 3. Instalar SDK components
Write-Host "Instalando SDK components..." -ForegroundColor Cyan
$sdkmanager = "$AndroidHome\cmdline-tools\latest\bin\sdkmanager.bat"

# Aceitar licenças
Write-Host "Aceitando Android SDK licenças..." -ForegroundColor Cyan
echo "y" | & $sdkmanager --licenses

# Instalar components
Write-Host "Instalando Android SDK 34, build tools..." -ForegroundColor Cyan
& $sdkmanager "platform-tools" "build-tools;34.0.0" "platforms;android-34" "platforms;android-24"

Write-Host "Setup completo!" -ForegroundColor Green
Write-Host "ANDROID_HOME foi definido como: $AndroidHome" -ForegroundColor Green

# Atualizar local.properties
$LocalProps = "C:\Users\7700924385\web\trabalho\appEmuladorTellnet\EmuladorTelnet\local.properties"
@"
sdk.dir=$AndroidHome
"@ | Out-File -FilePath $LocalProps -Encoding UTF8

Write-Host "local.properties atualizado" -ForegroundColor Green
Write-Host "`nAgora execute:" -ForegroundColor Cyan
Write-Host "`$env:ANDROID_HOME = '$AndroidHome'" -ForegroundColor White
Write-Host "`$env:JAVA_HOME = 'C:\Program Files\Amazon Corretto\jdk17.0.19_11'" -ForegroundColor White
Write-Host "cd C:\Users\7700924385\web\trabalho\appEmuladorTellnet\EmuladorTelnet" -ForegroundColor White
Write-Host ".\gradlew.bat assembleDebug" -ForegroundColor White
