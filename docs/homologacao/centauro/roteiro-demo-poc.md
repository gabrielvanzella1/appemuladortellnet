# Roteiro da demonstração à Centauro

**Data prevista:** 23/10/2026  
**Objetivo:** mostrar uma prova de conceito de reformatação de uma tela Telnet. A demonstração usa um servidor simulado; não acessa nem representa o ERP/SHARK real da Centauro.

## O que levar

- Coletor que será usado na reunião, carregado e com ScanTE Sandbox instalado.
- Notebook com este repositório, JDK/Android SDK configurados e Python disponível.
- Notebook e coletor na mesma rede Wi-Fi. Se não houver rede comum, combinar um hotspot.
- Um cabo USB para instalar o APK e, se necessário, transferir arquivos.

## Preparar antes da reunião

1. No repositório, confira `git status` e a branch `feature/reformatacao-tela-poc`.
2. Gere a build de demonstração:

   ```powershell
   cd EmuladorTelnet
   .\gradlew.bat assembleSandboxDebug
   ```

3. Instale no coletor e abra **ScanTE Sandbox**:

   ```powershell
   adb install -r app\build\outputs\apk\sandbox\debug\app-sandbox-debug.apk
   ```

4. Inicie o servidor simulado, a partir da raiz do repositório:

   ```powershell
   python scripts/demo_telnet_server.py 2323
   ```

5. Se o EDA estiver ligado por USB com depuração, encaminhe a porta sem alterar o Firewall:

   ```powershell
   adb reverse tcp:2323 tcp:2323
   ```

   Nesse caso, edite a sessão `Demo PoC` para host `127.0.0.1`, porta `2323`. Sem USB, o EDA e o notebook precisam estar na mesma rede e o host deve ser o IPv4 atual do notebook; a rede precisa permitir conexões TCP de entrada na porta 2323.
6. Conecte e confirme que aparece `RECEBIMENTO DE MERCADORIA`. A Sandbox deve trocar automaticamente para o formulário gráfico; não há opção de menu para ativar.

> A build Sandbox Debug tem sufixo de aplicativo próprio e painel de desbloqueio de teste em builds Debug. Use apenas para a demonstração controlada; não entregue esse APK como release oficial nem como atualização de produção.

## Apresentação (3–5 minutos)

1. Mostre primeiro a tela Telnet simulada em formato de terminal.
2. Aguarde a detecção automática e mostre o formulário nativo com identidade visual.
3. Preencha código `12345678`, quantidade `1` e local `A0101`, avançando com Enter.
4. Confirme com o botão ou F1. O servidor responde `RECEBIDO COM SUCESSO`.
5. Explique que a PoC reconhece uma tela por texto-chave e envia as teclas/campos pelo mesmo socket Telnet.

## O que dizer com precisão

- A tela e o servidor são uma demonstração criada para validar a interação; não são o sistema real da Centauro.
- Hoje o template é fixo e reconhece o texto `RECEBIMENTO DE MERCADORIA`; ainda não há editor de telas nem motor genérico que carregue configurações do painel.
- A adequação ao ambiente real depende de observar telas reais e confirmar protocolo, teclado e fluxos com o TI/usuários da Centauro.
- O gravador de telas é uma frente separada que pode ajudar a coletar exemplos autorizados. Não misturar a gravação real com a demonstração simulada.

## Perguntas para a reunião

- O host usa Telnet com VT220/ANSI ou emulação IBM 5250?
- Quais são as telas e os fluxos prioritários para o operador?
- É possível disponibilizar ambiente, host/porta e usuário de teste sem dados de produção?
- Quais teclas confirmam, cancelam e avançam em cada fluxo? Como o host indica erro (texto, BEL/bipe ou ambos)?
- Quais campos devem ser obrigatórios, numéricos ou associados ao leitor de código de barras?
- Há requisitos de marca, acessibilidade, orientação de tela ou modelos específicos de coletor?

## Se a conexão falhar

- Confirme que notebook e coletor estão na mesma rede e que o IP do notebook não mudou.
- Confira se o servidor ainda está rodando no terminal e se a porta `2323` está liberada na rede privada.
- Tente `Test-NetConnection <IP-DO-NOTEBOOK> -Port 2323` no próprio notebook para checar se há listener; depois valide do coletor pela conexão do app.
- Se a rede da empresa bloquear conexões locais, use um hotspot autorizado ou apresente o fluxo em uma rede de demonstração preparada.

## Gravação de telas reais — somente se autorizada

Para coletar material do SHARK real, obtenha antes do TI endpoint e usuário de teste. Peça autorização para registrar e compartilhar os snapshots. As telas gravadas podem conter dados ecoados pelo host; a máscara de senha é melhor esforço. Confira o JSON antes de enviá-lo. O procedimento detalhado está em [`guia-gravacao-telas.md`](guia-gravacao-telas.md).

## Estado de validação

A PoC foi descrita como testada anteriormente em um Honeywell EDA52. Em 22/10/2026, a branch combinada compilou com `assembleSandboxDebug`; o APK foi instalado no EDA52, conectou ao servidor pelo túnel ADB e exibiu automaticamente o formulário. A Production existente permaneceu instalada com a licença atual; o APK Production Release de atualização aguarda acesso à keystore oficial. O túnel USB serve apenas para testar este notebook; fora dele, a sessão de demonstração precisa alcançar um servidor disponível na rede.
