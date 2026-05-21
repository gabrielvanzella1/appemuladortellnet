# ========================================
# Git Setup Automatizado
# ========================================
# Execute este script na pasta raiz do projeto
# powershell.exe -File "git-setup.ps1"

param(
    [string]$RepoName = "appEmuladorTellnet",
    [string]$AuthorName = "Seu Nome",
    [string]$AuthorEmail = "seu@email.com"
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

# ========================================
# 1. Verificar Git
# ========================================
Write-Section "VERIFICANDO GIT"

if (Get-Command git -ErrorAction SilentlyContinue) {
    $gitVersion = git --version
    Write-Success "Git encontrado: $gitVersion"
} else {
    Write-Host "✗ Git não encontrado!" -ForegroundColor Red
    Write-Host "Instale com: winget install Git.Git" -ForegroundColor Magenta
    exit 1
}

# ========================================
# 2. Verificar se já tem .git
# ========================================
Write-Section "VERIFICANDO REPOSITÓRIO"

if (Test-Path ".git") {
    Write-Host "⚠ Repositório git já existe!" -ForegroundColor Yellow
    $response = Read-Host "Deseja continuar? (s/n)"
    if ($response -ne "s") {
        exit 0
    }
} else {
    Write-Step "Inicializando repositório git..."
    git init
    Write-Success "Repositório criado"
}

# ========================================
# 3. Configurar Git (local)
# ========================================
Write-Section "CONFIGURANDO GIT"

Write-Step "Seu nome para commits:"
$userName = Read-Host "Nome [$AuthorName]"
if ([string]::IsNullOrEmpty($userName)) { $userName = $AuthorName }

Write-Step "Seu email para commits:"
$userEmail = Read-Host "Email [$AuthorEmail]"
if ([string]::IsNullOrEmpty($userEmail)) { $userEmail = $AuthorEmail }

git config user.name $userName
git config user.email $userEmail

Write-Success "Git configurado localmente"
Write-Host "Nome: $userName" -ForegroundColor Green
Write-Host "Email: $userEmail" -ForegroundColor Green

# ========================================
# 4. Adicionar arquivos
# ========================================
Write-Section "PREPARANDO ARQUIVOS"

Write-Step "Verificando .gitignore..."
if (Test-Path ".gitignore") {
    Write-Success ".gitignore encontrado"
} else {
    Write-Host "⚠ .gitignore não encontrado!" -ForegroundColor Yellow
}

Write-Step "Adicionando arquivos ao index..."
git add .
Write-Success "Arquivos adicionados"

# ========================================
# 5. Verificar status
# ========================================
Write-Section "STATUS DO REPOSITÓRIO"

$status = git status --short
if ([string]::IsNullOrEmpty($status)) {
    Write-Host "⚠ Nenhum arquivo para commit" -ForegroundColor Yellow
} else {
    Write-Host $status -ForegroundColor Cyan
    Write-Host "`nTotal de mudanças:" -ForegroundColor Yellow
    $count = ($status | Measure-Object -Line).Lines
    Write-Host "$count arquivo(s)" -ForegroundColor Green
}

# ========================================
# 6. Primeiro commit
# ========================================
Write-Section "PRIMEIRO COMMIT"

$response = Read-Host "Deseja fazer o primeiro commit? (s/n)"
if ($response -eq "s") {
    git commit -m "chore: Initial commit - Emulador Telnet MVP

- Interface Telnet funcional
- Conexão básica com servidor Telnet
- Terminal com fundo preto e texto verde
- MVVM architecture
- Android API 24+

Status: MVP funcional, pronto para produção"
    
    Write-Success "Primeiro commit realizado!"
    
    # Mostrar log
    Write-Host "`nÚltimos commits:" -ForegroundColor Cyan
    git log --oneline -3
}

# ========================================
# 7. Instruções GitHub
# ========================================
Write-Section "PRÓXIMAS ETAPAS"

Write-Host @"

1. Criar repositório no GitHub:
   - Vá em: https://github.com/new
   - Nome: $RepoName
   - Descrição: "Android Telnet Emulator App"
   - NÃO inicialize com README (já temos)
   - Clique em "Create repository"

2. Adicionar remote (copie e cole):
   git remote add origin https://github.com/SEU_USER/$RepoName.git
   git branch -M main
   git push -u origin main

3. (Opcional) Configurar SSH:
   ssh-keygen -t ed25519 -C "seu@email.com"
   # Copie ~/.ssh/id_ed25519.pub para GitHub Settings > SSH Keys

4. Depois disso, pode fazer push com:
   git push
   git pull

"@ -ForegroundColor Green

# ========================================
# 8. Resumo
# ========================================
Write-Section "RESUMO"

Write-Host "✓ Git inicializado" -ForegroundColor Green
Write-Host "✓ .gitignore configurado" -ForegroundColor Green
Write-Host "✓ Arquivos adicionados" -ForegroundColor Green

$status = git status --porcelain
if ([string]::IsNullOrEmpty($status)) {
    Write-Host "✓ Tudo commitado" -ForegroundColor Green
} else {
    Write-Host "⚠ Ainda há mudanças não commitadas" -ForegroundColor Yellow
}

Write-Host "`nPróximo passo: Criar repositório no GitHub" -ForegroundColor Cyan
Write-Host "Pressione ENTER para sair..." -ForegroundColor Yellow
Read-Host
