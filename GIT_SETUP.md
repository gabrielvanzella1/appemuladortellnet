# 📦 Git Setup: Versionando Emulador Telnet no GitHub

## 🎯 O que vamos fazer

1. Inicializar Git local
2. Criar `.gitignore` (não trackear build, SDK, etc)
3. Fazer commit inicial
4. Criar repositório no GitHub
5. Fazer push

---

## ✅ Pré-requisitos

- Git instalado: `winget install Git.Git`
- Conta GitHub: https://github.com

---

## ⚡ Opção Rápida (RECOMENDADO)

Execute o script automatizado (faz tudo de uma vez):

```powershell
powershell.exe -File "git-setup.ps1"
```

Ele vai:
- ✓ Inicializar git
- ✓ Configurar seu nome/email
- ✓ Adicionar arquivos
- ✓ Fazer primeiro commit
- ✓ Mostrar próximos passos

---

## 📋 Opção Manual: Passo-a-Passo

### 1️⃣ Verificar Git

```powershell
git --version
# Output: git version 2.x.x
```

### 2️⃣ Inicializar Repositório

```powershell
cd "C:\Users\<seu-usuario>\web\trabalho\appEmuladorTellnet"
git init

# Verificar
git status
```

### 3️⃣ Configurar Git (Identidade)

```powershell
# Local (apenas este projeto)
git config user.name "Seu Nome Completo"
git config user.email "seu@email.com"

# Verificar
git config --local --list
```

**Opcional - Global (todos projetos):**
```powershell
git config --global user.name "Seu Nome"
git config --global user.email "seu@email.com"
```

### 4️⃣ Criar .gitignore

O arquivo `.gitignore` já foi criado! Ele exclui:
- `build/` - arquivos compilados
- `.gradle/` - cache gradle
- `*.apk` - APKs
- `.idea/` - IDE settings
- `local.properties` - paths locais
- `avd/` - emulator configs
- E mais...

**Verificar:**
```powershell
cat .gitignore
```

### 5️⃣ Adicionar Arquivos

```powershell
# Adicionar tudo (respeitando .gitignore)
git add .

# Ver o que vai ser adicionado
git status

# Output deve mostrar apenas arquivos necessários (verde com "new file:")
```

### 6️⃣ Primeiro Commit

```powershell
git commit -m "chore: Initial commit - Emulador Telnet MVP

- Interface Telnet funcional
- Conexão básica com servidor Telnet
- Terminal com fundo preto e texto verde
- MVVM architecture
- Android API 24+

Status: MVP funcional"
```

### 7️⃣ Ver Commits

```powershell
git log --oneline
# Output: abc1234 chore: Initial commit - Emulador Telnet MVP

git log --oneline --graph
```

---

## 🌐 Enviando para GitHub

### A. Criar Repositório no GitHub

1. Vá em: https://github.com/new
2. **Repository name:** `appEmuladorTellnet`
3. **Description:** `Android Telnet Emulator App with licensing support`
4. **Visibility:** Escolha (Public ou Private)
5. **NÃO** marque "Initialize with README" (já temos!)
6. Clique em **Create repository**

### B. Adicionar Remote

GitHub vai mostrar instruções. Copie e cole no PowerShell:

```powershell
# Configurar URL remota
git remote add origin https://github.com/SEU_USERNAME/appEmuladorTellnet.git

# Renomear branch para main
git branch -M main

# Fazer push inicial (primeira vez)
git push -u origin main
```

### C. Verificar Conexão

```powershell
# Ver remotes configurados
git remote -v

# Output:
# origin  https://github.com/SEU_USERNAME/appEmuladorTellnet.git (fetch)
# origin  https://github.com/SEU_USERNAME/appEmuladorTellnet.git (push)
```

---

## 🔐 Autenticação GitHub (Duas Opções)

### Opção 1: Token Pessoal (Recomendado para CI/CD)

```powershell
# GitHub pede token em vez de senha
# Gere em: https://github.com/settings/tokens

# Na primeira tentativa de push:
# - Username: seu-usuario
# - Password: [cole seu token aqui]
```

### Opção 2: SSH (Mais seguro)

```powershell
# Gerar chave SSH
ssh-keygen -t ed25519 -C "seu@email.com"

# Pressione ENTER para tudo (sem passphrase para simplificar)

# Copiar chave pública
cat $HOME\.ssh\id_ed25519.pub

# Ir em GitHub Settings > SSH and GPG keys > New SSH key
# Colar conteúdo
# Testar:
ssh -T git@github.com

# Depois, usar SSH URL:
git remote add origin git@github.com:SEU_USERNAME/appEmuladorTellnet.git
```

---

## 📤 Enviando Atualizações

Depois do primeiro push, use:

```powershell
# Ver mudanças
git status

# Adicionar
git add .

# Commit
git commit -m "feat: descrição da mudança"

# Push
git push
```

---

## 📊 Comandos Git Úteis

```powershell
# Status
git status                    # Ver mudanças
git diff                      # Ver diferenças em detalhe

# Histórico
git log                       # Ver todos commits
git log --oneline            # Ver resumido
git log --graph --all        # Ver com branches

# Branches
git branch                    # Listar branches
git branch nova-feature       # Criar branch
git checkout nova-feature     # Mudar para branch
git switch nova-feature       # Nova forma de mudar

# Merge
git merge nova-feature        # Mesclar branch em main
git rebase main              # Rebasear (mais limpo)

# Desfazer
git restore arquivo.txt      # Desfazer mudanças
git reset HEAD arquivo.txt   # Unstage arquivo
git revert abc123            # Desfazer commit

# Remote
git pull                     # Trazer mudanças
git fetch                    # Ver mudanças sem mesclar
git push                     # Enviar
```

---

## 🔄 Workflow Sugerido

### Para Novo Desenvolvedor (seu PC)

```
1. Clonar projeto:
   git clone https://github.com/SEU_USERNAME/appEmuladorTellnet.git
   cd appEmuladorTellnet

2. Executar setup:
   powershell.exe -File "setup-automatizado.ps1"

3. Criador uma branch para feature:
   git checkout -b feat/sua-feature

4. Fazer mudanças
   
5. Commit e push:
   git add .
   git commit -m "feat: descrição"
   git push -u origin feat/sua-feature

6. Criar Pull Request no GitHub

7. Merge e delete branch
```

### Para Colaboradores

```powershell
# Atualizar para latest
git pull origin main

# Criar feature branch
git checkout -b feat/nova-feature

# Fazer mudanças...

# Commit
git add .
git commit -m "feat: descrição clara"

# Push
git push -u origin feat/nova-feature

# No GitHub: Create Pull Request
# Reviewer aprova e faz merge
```

---

## 📝 Boas Práticas de Commit

### Formato de Mensagem (Conventional Commits)

```
<tipo>: <descrição breve>

<descrição detalhada opcional>

<rodapé opcional>
```

### Tipos

```
feat:    Nova funcionalidade
fix:     Correção de bug
docs:    Documentação
style:   Formatação de código
refactor: Reorganização sem mudança funcional
perf:    Melhoria de performance
test:    Adicionar/modificar testes
chore:   Tarefas (dependências, config, etc)
ci:      CI/CD
```

### Exemplos

```
✓ BOM:
feat: Implementar parser ANSI/VT100 para terminal
fix: Corrigir NullPointerException no scrollView
docs: Adicionar guia de setup

✗ RUIM:
fix stuff
atualizar arquivo
corrigido
```

---

## 🔍 Checklist: Git Setup

- [ ] Git instalado (`git --version` funciona)
- [ ] Repositório inicializado (`ls -la .git` existe)
- [ ] Nome/email configurados (`git config --local --list`)
- [ ] `.gitignore` criado
- [ ] Primeiro commit feito (`git log` mostra 1 commit)
- [ ] Repositório GitHub criado
- [ ] Remote configurado (`git remote -v`)
- [ ] Push realizado (`git push` funcionou)
- [ ] Verificar no GitHub se arquivos aparecem

---

## 🆘 Troubleshooting

| Problema | Solução |
|----------|---------|
| `fatal: not a git repository` | Execute `git init` |
| `error: src refspec main does not match any` | Fazer commit primeiro |
| `authentication failed` | Verificar token/SSH |
| `Permission denied (publickey)` | Configurar SSH key |
| `Your branch is ahead of 'origin/main'` | Executar `git push` |
| `merge conflict` | Editar arquivo e fazer `git add` |

---

## 📚 Referências

- [Git Official Docs](https://git-scm.com/doc)
- [GitHub Guides](https://guides.github.com)
- [Conventional Commits](https://www.conventionalcommits.org)

---

## 🚀 Próximos Passos

Após o Git setup:
1. ✅ Compartilhar URL do repositório
2. ✅ Convidar colaboradores (Settings > Collaborators)
3. ✅ Ativar Branch Protection (Settings > Branches)
4. ✅ Configurar CI/CD (Actions)
5. ✅ Adicionar Issues e Milestones

---

**Versão:** 1.0  
**Atualizado:** 21/05/2026  
**Status:** Guia Completo


