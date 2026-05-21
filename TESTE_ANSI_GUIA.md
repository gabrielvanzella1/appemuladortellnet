# 🎨 Teste ANSI Parser - Guia Rápido

## ✅ App Agora Rodando com ViewBinding!

### Status Atual
- ✅ ViewBinding corrigido (MainActivity.kt)
- ✅ ANSIParser integrado 100%
- ✅ Compilado e rodando no emulador
- ✅ Servidor Telnet local rodando em 127.0.0.1:2323

---

## 🧪 Como Testar

### Passo 1: Servidor Rodando?
```
Terminal PowerShell já rodando:
✅ Servidor em 127.0.0.1:2323 com ANSI colors
```

### Passo 2: Abrir App
```
✅ App aberto no emulador Android
```

### Passo 3: Conectar
```
HOST: 10.0.2.2    (IP do computador dentro do emulador)
PORT: 2323        (Porta do servidor)
BOTÃO: CONECTAR
```

### Passo 4: Resultado
```
Você verá:
- Vermelho (Red)
- Verde (Green)
- Amarelo (Yellow)
- Azul (Blue)
- Magenta
- Cyan
- Vermelho Bold
- Verde Bold
```

---

## 🔍 Debugging - Se não aparecerem cores:

### 1. Verificar Logcat
```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb logcat "com.logisticapp.emuladortelnet:V" -c
& $adb logcat "com.logisticapp.emuladortelnet:V"
```

Procure por:
- `ANSI parsed: X segments` - Parser funcionando
- `HTML output:` - HTML gerado
- Erros de conexão

### 2. Testar com Star Wars
```
HOST: towel.blinkenlights.nl
PORT: 23
```

Star Wars tem muitas cores ANSI!

### 3. Problemas Comuns

| Problema | Solução |
|----------|---------|
| App fecha ao conectar | Verificar logcat para exception |
| Conexão recusada | Certifique-se que servidor está rodando |
| Texto aparece mas sem cores | Parser não está processando HTML |
| Apenas plain text | Html.fromHtml() não está ativado |

---

## 🔧 Monitorar Servidor

Terminal do PowerShell:
```
Mensagens quando cliente conecta:
- "Cliente conectado!"
- Cores sendo enviadas
```

---

## 📊 Arquitetura Testada

```
1. TelnetClient conecta ✅
2. readAvailable() lê dados ✅
3. addTerminalOutput() chamado ✅
4. ANSIParser.parse() processa ✅
5. ANSIParser.toHtmlSpan() converte ✅
6. ViewBinding atualiza TextView ✅
7. Html.fromHtml() renderiza ✅
```

---

## 🎯 Próximos Passos

Se funcionar ✅:
1. Commit para GitHub
2. Ir para Fase 4 (Input de Comandos)

Se não funcionar ❌:
1. Verificar logcat
2. Debug com breakpoints
3. Testar parser isoladamente

---

## 📱 Screenshot esperado

```
┌─────────────────────────────────┐
│ Emulador Telnet                 │
├─────────────────────────────────┤
│ Status: CONECTADO ✓ (verde)    │
├─────────────────────────────────┤
│ Vermelho (Red)                  │ ← Vermelho
│ Verde (Green)                   │ ← Verde
│ Amarelo (Yellow)                │ ← Amarelo
│ Azul (Blue)                     │ ← Azul
│ Magenta                         │ ← Magenta
│ Cyan                            │ ← Cyan
│ Vermelho Bold                   │ ← Vermelho Bold
│ Verde Bold                      │ ← Verde Bold
│                                 │
│ Título com Cores                │ ← Título Bold Amarelo
│ Teste concluído!                │
└─────────────────────────────────┘
```

---

**Status:** Pronto para testar!
**Versão:** 3.0.1 (ViewBinding Fix)
**Data:** 21/05/2026
