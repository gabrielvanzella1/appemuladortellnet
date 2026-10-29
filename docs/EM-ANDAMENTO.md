# Em andamento

> Para ajustes em campo na Centauro (29–30/10/2026): leia primeiro [GUIA-AJUSTES-CAMPO.md](GUIA-AJUSTES-CAMPO.md).

## Estado em 22/10/2026 — preparação para demonstração à Centauro

- Branch local: `feature/reformatacao-tela-poc`.
- Commit atual: merge local `453547d`, que integra `old-origin/feature/gravador-telas`.
- A branch de trabalho trouxe o gravador de telas, a tela de controle e a exportação das gravações.
- A branch local também contém a PoC de reformatação visual para uma tela de demonstração.
- A build `assembleSandboxDebug` compilou com sucesso em 22/10/2026 e foi instalada/aberta no Honeywell EDA52. A conexão ao servidor simulado funcionou pelo túnel ADB e a interface gráfica apareceu automaticamente.
- A build `assembleRecorderDebug` compilou em 22/10/2026, foi instalada em paralelo no EDA52 (`com.logisticapp.emuladortelnet.recorder`) e abriu a tela de licença sem crash. Não foi ativada para preservar a licença da instalação Production; a tela do gravador ainda precisa ser conferida após ativação autorizada.
- Builds 1.1.4 do Gravador e da Sandbox foram compiladas em 22/10 com o limite multi-host; ainda não foram instaladas no EDA para validação dessa mudança.
- 23/10: corrigido erro "USUARIO SEM PERMISSAO DE CANCELAR CONTAGEM" no Urovo. Causa provada por log (Enter do leitor saia da rede antes do código; cada `sendRaw` era uma coroutine solta). Correção: fila única em `TelnetViewModel.sendRaw`. Teste real no Urovo: 182 envios, 0 trocas de ordem, 0 erros. APKs com a correção na pasta de entrega com sufixo `fix-ordem`; os sem sufixo NÃO têm a correção.
- 23/10: atrasos relatados nas setas na consulta: não reproduzidos. Medido: app leva ~3 ms (máx 11) para gravar na rede; seta -> resposta ~45 ms; rede Urovo -> `wmascltprd08` 38 ms de média, 0% de perda. Respostas lentas (~800 ms) foram Enters de confirmação (host). Causa dos atrasos da operadora não confirmada.
- Pasta para levar os APKs: `C:\Users\Usuario\Desktop\centauro23-10` — normal 1.1.2 (20/10), Gravador 1.1.4 e Sandbox demonstrativa 1.1.4.
- O EDA52 mostra os pacotes `com.logisticapp.emuladortelnet`, `.recorder` e `.sandbox` instalados. A instalação Production e a licença existentes foram preservadas.
- O APK Production Release ainda não foi gerado: `EmuladorTelnet/keystore.properties` e a chave oficial não estão nesta máquina; a autenticação GitHub CLI para o repositório privado está inválida.
- A branch local está à frente de `old-origin/feature/reformatacao-tela-poc`; o merge ainda não foi enviado ao remoto.

## Separação importante para a apresentação

- O emulador Telnet permite até 8 conexões simultâneas; o botão `⇄` no terminal percorre os hosts ativos e mostra a posição atual/total.

1. **PoC de reformatação:** conectar ao servidor simulado `scripts/demo_telnet_server.py`; ao receber `RECEBIMENTO DE MERCADORIA`, a Sandbox abre o formulário automaticamente. A tela é fictícia e não representa o ERP da Centauro.
2. **Gravador de telas:** só gravar o SHARK real com autorização, host/porta fornecidos pelo TI e usuário de teste. Conferir a gravação antes de compartilhar; telas ecoadas pelo host podem conter dados.

Roteiro completo: [`docs/homologacao/centauro/roteiro-demo-poc.md`](homologacao/centauro/roteiro-demo-poc.md).

## Build e segurança

- A Sandbox Debug tem painel de desbloqueio de teste e assinatura de debug (SHA-256 `47fb9332…`). Use-a só para a demonstração controlada; não a distribua como release oficial.
- O Gravador Debug (`app-recorder-debug.apk`) também tem assinatura de debug e pacote `.recorder`, para coexistir com Production durante homologação. A licença dele fica em dados separados; não foi ativado neste teste.
- APK Sandbox gerado em `EmuladorTelnet/app/build/outputs/apk/sandbox/debug/app-sandbox-debug.apk` (regerado após habilitar detecção automática). Build, instalação, conexão e exibição automática foram conferidas no EDA52.
- Para atualizar Production sem desinstalar nem perder a licença existente, recuperar a keystore oficial do repositório privado, criar `EmuladorTelnet/keystore.properties`, gerar `assembleProductionRelease` e conferir o certificado SHA-256 oficial `c8eef2b7…` antes de instalar.
- Não usar a PoC para operar o sistema real da Centauro: o template é fixo e só reconhece o texto exato da tela demonstrativa.

## Próximo passo

Reautenticar GitHub CLI para obter a keystore privada; depois gerar e conferir o Production Release para atualização do app existente. Percorrer também o roteiro de demonstração Sandbox no EDA52. Para gravar fluxo real, confirmar previamente acesso, licença, usuário de teste e endpoint com o TI da Centauro.
