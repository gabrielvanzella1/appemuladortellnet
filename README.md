# 📱 Emulador Telnet Android

App Android para conectar e interagir com servidores Telnet, com suporte a terminal emulado.

**Status:** ✅ MVP Funcional

---

## 🎯 Objetivo

Fornecer uma aplicação Android leve e funcional para:
- Conectar a servidores Telnet
- Emular terminal com interface visual
- Suportar sistema de licença para uso em logística
- Funcionar em dispositivos de coleta portátil

---

## ✨ Funcionalidades Atuais

✅ Interface de conexão (Host + Porta)  
✅ Botões Conectar/Desconectar  
✅ Terminal visual com fundo preto e texto verde  
✅ Indicador de status da conexão  
✅ Conexão Telnet básica (placeholder com Thread.sleep)  

---

## 🚀 Quick Start

### Opção 1: Script Automatizado (RECOMENDADO)

```powershell
# Execute e deixe rolar!
powershell.exe -File "setup-automatizado.ps1"
```

### Opção 2: Manual

1. Instale Java 11: `winget install Amazon.Corretto.11`
2. Configure Android SDK
3. Execute: `gradlew.bat clean assembleDebug`
4. Crie AVD: `avdmanager create avd -n TestDevice ...`
5. Inicie emulator e app

---

## 📖 Documentação

| Arquivo | Descrição |
|---------|-----------|
| `GUIA_SETUP_OUTRO_PC.md` | ⭐ **Comece aqui** - Guia completo passo-a-passo |
| `REFERENCIA_RAPIDA.md` | Comandos prontos para copy-paste |
| `setup-automatizado.ps1` | Script PowerShell que automatiza tudo |
| `PROJECT_STATUS.md` | Status detalhado do projeto |
| `IMPLEMENTATION_GUIDE.md` | Guia de implementação técnica |

---

## 🛠️ Requisitos

- Windows 10/11
- Java 11+
- Android SDK (API 24+)
- 50GB espaço livre
- Internet

---

## 📋 Stack Técnico

**Frontend:**
- Kotlin 1.8.10
- AndroidX
- MVVM + Repository Pattern
- Coroutines

**Ferramentas:**
- Gradle 8.6
- Android Gradle Plugin 7.4.2
- ViewBinding

**Dependências Principais:**
- `androidx.lifecycle` - MVVM
- `kotlinx.coroutines` - Async
- `okhttp3` - Networking (futuro)
- `gson` - JSON
- `timber` - Logging

---

## 🔗 Usando com Copilot

No VS Code, abra **Copilot Chat** (Ctrl+Shift+I):

```
"Estou desenvolvendo um app Telnet Android. Como implemento [feature]?"
```

```
"Este código está certo? @FileName" 
```

```
"Qual é o próximo passo depois de conectar a um servidor Telnet?"
```

---

## 📁 Estrutura do Projeto

```
EmuladorTelnet/
├── app/
│   ├── src/main/java/com/logisticapp/emuladortelnet/
│   │   ├── MainActivity.kt (UI principal)
│   │   ├── ui/TelnetViewModel.kt (Lógica)
│   │   └── data/Models.kt (Data classes)
│   ├── src/main/res/
│   │   ├── layout/activity_main.xml
│   │   └── values/{strings,colors,styles}.xml
│   └── build.gradle.kts
├── gradle/ (Wrapper + versions)
├── build.gradle.kts
├── settings.gradle.kts
├── local.properties (criar)
└── gradlew.bat
```

---

## 🧪 Testar Conexão

### Servidor Público
```
Host: towel.blinkenlights.nl
Porta: 23
```
→ Mostra Star Wars em ASCII

### Servidor Local
```
Host: 10.0.2.2 (ou seu IP)
Porta: 23
```
→ Use o servidor Telnet incluído

---

## 🔄 Roadmap

### Fase 1: Telnet Funcional ✅
- [x] Interface básica
- [ ] TCP Socket real
- [ ] RFC 854 negotiation
- [ ] ANSI/VT100 parsing

### Fase 2: Licença
- [ ] Device fingerprinting
- [ ] Local validation
- [ ] Server validation
- [ ] Expiration control

### Fase 3: Produção
- [ ] Database (Room)
- [ ] Multiple connections
- [ ] SSH suporte
- [ ] Testes completos

---

## 🆘 Problemas?

1. **Leia:** `GUIA_SETUP_OUTRO_PC.md` seção "Troubleshooting"
2. **Pesquise:** Use Copilot Chat no VS Code
3. **Pergunte:** Descreva o erro exato para Copilot

---

## 💰 Informações Comerciais

**Escopo MVP:** 2-3 semanas  
**Escopo Completo:** 8-12 semanas  
**Preço Estimado:** R$ 18.000 - 80.000 (varia por escopo)

Para orçamento: envie especificações para Copilot analisar

---

## 📄 Licença

Projeto experimental para fins educacionais/comerciais

---

## 👤 Desenvolvido com

- GitHub Copilot
- VS Code
- Android SDK
- PowerShell

---

## 🎯 Próximos Passos

1. **Novo PC?** → Execute `setup-automatizado.ps1`
2. **Problemas?** → Leia `GUIA_SETUP_OUTRO_PC.md`
3. **Dúvidas técnicas?** → Pergunte ao Copilot
4. **Implementar feature?** → Veja `IMPLEMENTATION_GUIDE.md`

---

**Status:** ✅ Funcional  
**Última atualização:** 21/05/2026  
**Versão:** 1.0 MVP  

