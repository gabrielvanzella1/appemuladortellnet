# Prepara a conta de demonstração (empresa "Scan TE Produção", id 4) para mostrar a Cerca digital.
#   1. recria os 8 coletores simulados (SIM-01..08) e as 5 zonas do "CD"  — no servidor
#   2. gera o histórico de hoje (linha do tempo)                           — pela API
#   3. mantém 7 coletores "vivos" (1 sinal/min); o SIM-08 fica calado e vira alerta "sem comunicação"
#
# Uso (PowerShell, ~10 min antes da apresentação):
#   .\scripts\preparar-demo-cerca.ps1               # 120 min ao vivo
#   .\scripts\preparar-demo-cerca.ps1 -Minutos 180
# Deixe a janela aberta durante a apresentação; fechar = coletores param de mandar sinal.
# Não mexe na Centauro nem em nenhum coletor real: só apaga/recria o que começa com SIM-.
param([int]$Minutos = 120)
$ErrorActionPreference = 'Stop'

# Caminhos relativos ao próprio repositório (funciona em qualquer máquina que tenha o clone)
$raiz   = Split-Path $PSScriptRoot -Parent
$admin  = Join-Path $raiz 'scante-admin'
$apiKt  = Join-Path $raiz 'scante-monitor\app\src\main\java\com\logisticapp\scantemonitor\Api.kt'
# PHP: o do PATH; senão, o do Laragon
$php    = (Get-Command php -ErrorAction SilentlyContinue).Source
if (-not $php) { $php = Get-ChildItem 'C:\laragon\bin\php\*\php.exe' -ErrorAction SilentlyContinue | Select-Object -Last 1 -ExpandProperty FullName }
if (-not $php) { throw "PHP nao encontrado (instale o PHP ou o Laragon)" }
$remoto = 'cd ~/domains/scante.com.br/public_html/scante-admin && php -- --empresa=4'

# URL e segredo lidos do próprio app (não são impressos)
$kt     = Get-Content $apiKt -Raw
$base   = [regex]::Match($kt, 'BASE_URL\s*=\s*"([^"]+)"').Groups[1].Value
$secret = [regex]::Match($kt, 'API_SECRET\s*=\s*"([^"]+)"').Groups[1].Value
if (-not $base -or -not $secret) { throw "Nao consegui ler BASE_URL/API_SECRET de $apiKt" }

Write-Host "1/3 Recriando coletores simulados no servidor..." -ForegroundColor Cyan
# -RedirectStandardInput manda o arquivo byte a byte (o pipe do PowerShell 5 estragaria os acentos)
$p = Start-Process ssh -ArgumentList '-o', 'BatchMode=yes', 'hostinger-scante', $remoto `
     -RedirectStandardInput "$admin\tools\seed_cerca_demo.php" -NoNewWindow -Wait -PassThru
if ($p.ExitCode -ne 0) { throw "Falhou no servidor (ssh saiu com $($p.ExitCode))" }

Write-Host "2/3 Gerando o historico de hoje..." -ForegroundColor Cyan
& $php "$admin\tools\simular_cerca.php" "--url=$base" "--secret=$secret"
if ($LASTEXITCODE -ne 0) { throw "Falhou ao gerar o historico" }

Write-Host "3/3 Coletores ao vivo por $Minutos min (feche a janela para parar)..." -ForegroundColor Green
Write-Host "    Painel: $base  ->  login da conta de demonstracao (cerca-teste@scante.com.br)"
& $php "$admin\tools\simular_cerca.php" "--url=$base" "--secret=$secret" "--ao-vivo=$Minutos"
