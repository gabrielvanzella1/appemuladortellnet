# Manual do ScanTE

**ScanTE — Conecta · Sincroniza · Simplifica**
Software de comunicação entre coletores de dados e sistemas corporativos via terminal (Telnet).

> Este manual descreve **cada funcionalidade** do aplicativo, para que serve e como usar.
> Legenda de status: ✅ funcional · 🔧 em desenvolvimento

---

## 1. Visão geral

O ScanTE é um emulador de terminal **Telnet** para Android. Ele permite que coletores de
dados (e celulares) se conectem a sistemas corporativos (Protheus/TOTVS, AS/400, entre
outros) que funcionam por linha de comando / tela de texto, exibindo a tela do sistema e
permitindo digitar e navegar como num terminal de verdade.

**Fluxo básico de uso:**
1. Abrir o app → tela de **Licença** (período de teste ou licença vitalícia)
2. **Sessões** → lista de servidores cadastrados
3. Criar/abrir uma sessão → **Terminal** conectado ao sistema

---

## 2. Tela de Licença ✅

É a primeira tela ao abrir o app.

- **Status / Dias restantes / Tipo:** mostra a situação da licença (Teste Gratuito por
  tempo limitado, ou Licença Vitalícia após a compra).
- **Continuar:** entra no app (disponível durante o teste ou com licença válida).
- **Comprar Licença:** inicia a compra da licença vitalícia (via Mercado Pago).

---

## 3. Tela de Sessões ✅

Lista os servidores (sessões) cadastrados. Cada item mostra o **nome** e o **endereço:porta**.

- **Tocar numa sessão:** abre a configuração dela (nome, IP, porta).
- **Botão + (canto inferior):** cria uma nova sessão.
- **3 pontinhos de cada sessão:** Conectar, Editar, Configuração avançada, Remover.

### Menu geral (3 pontos no topo) ✅
Ações que valem para o app / sessões:

| Opção | Para que serve |
|---|---|
| **Configurações** | Abre o painel de configurações (Comunicação, Emulação, Tela, Dispositivos) |
| **Novo** | Cria uma nova sessão |
| **Remover** | Apaga uma sessão escolhida |
| **Renomear** | Muda o nome de uma sessão |
| **Ajuda** | Texto de ajuda rápida |
| **Importação** | Importa sessões de um arquivo (.json) |
| **Exportação** | Compartilha/salva as sessões num arquivo (.json) — backup |
| **Modelos** | 🔧 Modelos prontos de sessão |
| **Opções gerais** | Configurações de comportamento do app (ver seção 7) |
| **Sobre ScanTE** | Versão e informações do app |

---

## 4. Tela do Terminal ✅

É onde você vê e opera o sistema conectado.

- **Digitação direta:** toque na tela do terminal (ou no ícone de teclado ao lado de
  "Desconectar") para abrir o teclado. O que você digita vai **direto** para o sistema —
  é assim que se preenche Usuário, Senha e comandos.
- **Ícone de teclado:** mostra/oculta o teclado virtual.
- **Desconectar:** encerra a conexão.
- **Barras de ferramentas:** botões de atalho (setas, Enter, Ctrl+letra, etc.) configuráveis
  (ver seção 6.3).
- **Campos de preenchimento destacados:** os campos editáveis do sistema (ex: Usuário,
  Senha) aparecem com cor de fundo, para você ver onde digitar.

---

## 5. Configurações → Comunicação

### 5.1. Telnet Opções

Parâmetros de como o app conversa com o servidor. **As Telnet Opções são globais** (valem
para o app todo).

#### Conexão
| Função | Para que serve | Status |
|---|---|---|
| **Endereço do servidor** | Mostra o endereço:porta do servidor (vem da sessão cadastrada; é só leitura — para mudar, edite a sessão). | ✅ |
| **Tipo de terminal** | Informa ao sistema "que terminal você é" (VT100, VT220, ANSI…). Muda como o sistema desenha a tela. O Protheus normalmente usa VT220. | ✅ |
| **Terminador de linha** | O que é enviado quando você aperta **Enter**: `CR+LF`, `CR` ou `LF`. Se o Enter "não avança" a tela, troque aqui (muitos ERPs usam só `CR`). | ✅ |
| **Usar IP para BRK** | A tecla **Break** passa a enviar "Interromper Processo" (aborta o comando atual, tipo um Ctrl+C) em vez do Break comum. Útil em AS/400 e mainframes. | ✅ |
| **Modo binário** | Liga a transmissão de 8 bits (caracteres completos). Use quando precisar de **acentos** (ç, ã, é) corretos. | ✅ |
| **Simular paridade** | Remove o 8º bit dos dados recebidos (trata como 7 bits). Use quando aparecem **caracteres estranhos** por causa de bit de paridade. ⚠️ Não use junto com Modo binário (são opostos). | ✅ |
| **Mantenha o tipo vivo** | Mantém a conexão ativa (keep-alive) para não cair por inatividade. **TCP** = usa o keep-alive do sistema. **NVT** = o app envia um sinal invisível (NOP) periodicamente. **Desligado** = sem keep-alive. | ✅ |
| **Mantenha o intervalo vivo** | De quantos em quantos **segundos** o sinal de keep-alive NVT é enviado (ex: 60). Vale quando o tipo é NVT. | ✅ |

#### Login Telnet (login automático) ✅
Sequência para o app fazer login sozinho: aguarda o pedido de login → envia o usuário →
aguarda o pedido de senha → envia a senha → aguarda o prompt → envia um comando.

**Como funciona:** Você preenche os 6 campos (prompts e credenciais) na tela Telnet Opções.
Ao conectar, o app monitora o texto que chega do servidor. Quando encontra o **"Aguarde o 
prompt de login"** (ex: `login:`), envia automaticamente o valor de **"Faça login com"** 
(usuário). Depois aguarda **"Aguarde a solicitação de senha"** e envia a **"Senha"**. 
Se um **"Aguarde o prompt de comando"** for configurado, aguarda e envia **"Faça o comando"**.

**Exemplo:** Para um Protheus que pede `login:`, `Password:` e depois mostra `>`, configure:
- Aguarde o prompt de login: `login:`
- Faça login com: `seu_usuario`
- Aguarde a solicitação de senha: `Password:`
- Senha: `sua_senha`
- Aguarde o prompt de comando: `>`
- Faça o comando: `` (deixe vazio se não quer enviar nada após login)

#### Sockets seguros (SSL) ✅
Conexão criptografada via TLS — os dados entre o app e o servidor ficam protegidos.

| Campo | Para que serve |
|---|---|
| **Sockets seguros (SSL)** | Liga/desliga a criptografia TLS. Quando marcado, a conexão usa TLS em vez de Telnet puro. Necessário quando o servidor exige porta segura (ex: 992). |
| **Autenticar certificado do servidor** | (Configuração reservada para versão futura — atualmente o app aceita qualquer certificado do servidor automaticamente). |
| **Arquivo de certificado cliente** | Caminho para um arquivo `.p12` ou `.pfx` no dispositivo — usado quando o servidor exige que **você** se identifique com um certificado. Deixe vazio se não precisar. |
| **Senha do certificado cliente** | Senha de proteção do arquivo `.p12/.pfx`, se houver. |

**Quando usar:** Servidores Telnet/S (Telnet over SSL), ou conexões em que a empresa exige canal criptografado (ex: auditoria, dados financeiros).

#### Conexão SSH ✅
Conexão via protocolo SSH — canal criptografado e autenticado, alternativa ao Telnet puro.

| Campo | Para que serve |
|---|---|
| **Conexão SSH** | Liga/desliga o modo SSH. Quando marcado, o app conecta via SSH em vez de Telnet. |
| **Servidor SSH** | Endereço e porta do servidor SSH (ex: `192.168.1.10:22`). Se vazio, usa o endereço da sessão. |
| **Usuário SSH** | Nome de usuário para autenticar no servidor SSH. |
| **Senha SSH** | Senha do usuário (autenticação por senha). |
| **Chave privada SSH** | Caminho para o arquivo de chave privada no dispositivo (PEM/OpenSSH). Deixe vazio para autenticar só por senha. |
| **Keep-alive SSH** | Intervalo em segundos para envio de sinal de keep-alive SSH (ex: 60). Deixe 0 para desativar. |

**Quando usar:** Quando o servidor não aceita Telnet puro e exige SSH (porta 22), ou quando a empresa requer canal autenticado e criptografado.

### 5.2. Servidor proxy ✅
Conectar através de um servidor intermediário (proxy). Útil quando o dispositivo está numa
rede que não acessa o servidor Telnet/SSH diretamente e exige passagem por um proxy corporativo.

| Campo | Para que serve |
|---|---|
| **Usar servidor proxy** | Liga/desliga o redirecionamento pelo proxy. |
| **Endereço do proxy** | IP ou nome do servidor proxy (ex: `192.168.1.1` ou `proxy.empresa.com`). Para especificar a porta direto no endereço, use `host:porta`. |
| **Porta** | Porta do proxy (padrão: 30855). Usada se não informada no campo de endereço. |
| **Comunicação segura** | Usa HTTPS ao conectar no proxy (em vez de HTTP simples). |
| **Manter conexão quando usuário desconectar** | Segundos que o proxy mantém o canal com o servidor após você desconectar (0 = fecha imediatamente). |
| **Manter conexão quando conexão for perdida** | Segundos que o proxy aguarda antes de encerrar o canal quando a conexão cai inesperadamente (padrão: 300). |

**Como funciona:** O app abre uma conexão TCP com o proxy e envia um comando `HTTP CONNECT` pedindo
ao proxy para abrir um túnel até o servidor destino. A partir daí, o tráfego Telnet ou SSL passa
por dentro desse túnel de forma transparente.

---

## 6. Configurações → Tela

### 6.1. Opções de tela ✅ (parcial)
| Item | Para que serve | Status |
|---|---|---|
| **Tamanho da fonte** | Tamanho do texto no terminal | ✅ |
| **Cursor piscando** | Liga/desliga a piscada do cursor | ✅ |
| **Campos 3D de fundo branco** | Estilo dos campos | ✅ (salvo) |
| Nome da fonte, Tipo/Cor do cursor, Campos variáveis 3D, Mostrar barra de ferramentas, Limitar visualização, Toque duas vezes | Demais ajustes de exibição | 🔧 |

### 6.2. Cores da tela ✅
- **Primeiro plano:** cor do texto do terminal.
- **Plano de fundo:** cor de fundo do terminal.
- **Status em primeiro plano / Plano de fundo do status:** cores da barra superior.
- **Campos de preenchimento:** cor de destaque dos campos editáveis (Usuário, Senha…).
- *Ajuste de cor (escuro/brilhante):* 🔧

### 6.3. Configuração da barra de ferramentas ✅
São **4 barras** de botões que aparecem no terminal. Cada botão envia uma tecla/comando
(setas, Enter, Esc, Ctrl+letra, texto, Connect/Disconnect…).
- **Adicionar:** escolhe a barra e adiciona botões prontos (Find, Select, Copy, Paste, etc.).
- **Tocar num botão da lista:** remove.

### 6.4. Opções de tela / VT Mapeamento de atributos 🔧

---

## 7. Opções gerais ✅
| Opção | Para que serve |
|---|---|
| **Orientação da tela** | Retrato, Paisagem ou Automático |
| **Conexão automática na inicialização** | Conecta sozinho na última sessão ao abrir o app |
| **Reconectar após conexão perdida** | Reconecta sozinho se a conexão cair sem ser por você |
| **Desconectar na tela de bloqueio** | Encerra a conexão quando o celular bloqueia |
| **Nunca bloquear a tela quando conectado** | Mantém a tela acesa durante o uso |
| **Ignorar otimização de bateria** | Evita que o Android "durma" o app |
| **Teclado ativado** | Mostra/oculta a área de digitação no terminal |

---

## 8. Configurações → Emulação 🔧
- **VT Opções:** ajustes finos do terminal VT.
- **Transliteração:** substituição/conversão de caracteres.
- **Geral:** opções gerais de emulação.

## 9. Configurações → Dispositivos 🔧
- **Configuração de impressão**
- **Configuração do leitor de código de barras**

---

## 10. Glossário rápido

- **Telnet:** protocolo de terminal por texto (RFC 854).
- **CR / LF / CR+LF:** caracteres invisíveis de "fim de linha" enviados ao apertar Enter.
- **Tipo de terminal (VT100/VT220):** "modelo" de terminal que o sistema usa para desenhar a tela.
- **Modo binário:** transmissão de 8 bits (necessário para acentos).
- **Paridade:** bit extra de verificação em links antigos de 7 bits.
- **Keep-alive:** mensagens periódicas para manter a conexão viva.
- **Proxy:** servidor intermediário entre o app e o sistema.

---

*Documento atualizado conforme novas funcionalidades são concluídas.*
