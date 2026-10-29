# Pesquisa técnica — disponibilidade de recursos no Zion WMS

**Data da pesquisa:** 23/10/2026  
**Escopo:** auditoria somente leitura do repositório e fontes públicas do fornecedor. Nenhum código ou fluxo do app foi alterado.

## Diagnóstico e recomendação

O ScanTE atual é um cliente/emulador de terminal para Android. Nesta revisão não foi encontrado cadastro de recursos operacionais (tarefas, filas ou postos de trabalho do Zion), nem modelo, tabela, API ou serviço que mantenha status de recurso como livre/ocupado. O que existe é cadastro local de **conexões Telnet** (nome, host, porta e opções), sessões ativas e logs locais. Isso não é a mesma entidade que um recurso de trabalho do WMS.

É tecnicamente plausível alcançar a experiência solicitada, mas a disponibilidade real não pode ser demonstrada com o código atual nem com a informação pública consultada. A recomendação é solicitar à Zion e ao cliente uma interface **suportada e transacional** para listar recursos elegíveis e assumir/obter um recurso no momento da seleção. Reaproveitar `HostsActivity`/`SavedConnection` para guardar hosts e a conexão Telnet existente para o terminal; introduzir um adaptador separado do WMS só após confirmar protocolo, autenticação, semântica e suporte do fornecedor. Não manter uma reserva local paralela.

Se o fornecedor não tiver API, serviço ou consulta autorizada, uma extensão do fluxo terminal pode ser avaliada em homologação: interpretar as telas/respostas que o próprio usuário autorizado recebe e encaminhar a seleção pelo mesmo fluxo operacional do Zion. Isso depende de conhecer e validar as telas e respostas reais; não se recomenda automatizar scraping ou inferir estados a partir de texto sem confirmação do fornecedor. Se não houver operação de seleção com confirmação atômica no Zion, a lista só pode ser informativa e não deve afirmar que garante disponibilidade.

### Fluxo operacional proposto usando a sessão Telnet

O fluxo descrito pelo cliente — login normal, comando “Encontrar recurso disponível”, navegação automatizada e seleção pelo caminho normal — é uma hipótese concreta para uma prova em homologação. Pode aproveitar a sessão autenticada sem pedir uma segunda credencial, mas ainda não está comprovado que as telas ou comandos do ambiente Zion permitam esse percurso sem efeitos colaterais.

1. O operador conecta ao Zion e faz login como hoje. O ScanTE não captura nem reutiliza a senha para outra integração.
2. O operador inicia a busca por ação explícita. O ScanTE guarda a tela atual e valida que a sessão está conectada e num ponto de navegação conhecido.
3. Um interpretador de fluxo envia somente teclas/comandos de navegação validados para percorrer o conjunto delimitado. A máquina de estados espera a resposta/tela esperada após cada passo. Timeout, prompt desconhecido, sessão perdida ou resposta fora do padrão interrompe a busca; não se enviam teclas cegamente.
4. Para cada recurso, o parser associa código, descrição (se exibida), resposta do Zion, estado interpretado e instante da leitura. Só uma resposta conhecida e validada pode marcar um item como candidato. Resposta ambígua ou inesperada fica como desconhecida/erro, nunca como livre.
5. A lista mostra candidatos encontrados naquela varredura e a hora em que foram observados. A listagem não reserva itens; outro operador pode alterar o estado.
6. Ao escolher, o ScanTE reentra no fluxo normal do Zion para aquele código e envia a ação de seleção prevista pelo sistema. Só informa “selecionado” após reconhecer confirmação positiva do Zion. Se o Zion responder conflito/indisponível, atualiza a lista e permite outra escolha.
7. Ao concluir, cancelar ou falhar, o fluxo volta a uma tela segura e conhecida (idealmente a tela anterior), ou informa claramente onde ficou. Não deve deixar teclas pendentes nem a sessão em estado incerto.

**Limite de consistência:** candidatos são uma fotografia, não promessa de disponibilidade. A janela entre leitura e seleção continua sujeita à concorrência. A confirmação final do Zion é obrigatória; sem resposta inequívoca de seleção aceita/rejeitada, o ScanTE não pode garantir que o operador obteve o recurso. Não selecionar automaticamente outro recurso após resposta ambígua, pois isso pode assumir trabalho inesperado.

**Condições de segurança:** sequência de navegação documentada/autorizada; telas e transições identificáveis; navegação não destrutiva; limites de varredura; pausas e cancelamento; tratamento de tela parcial e modal; mensagens de sucesso, conflito e erro conhecidas; saída/fallback testado. Se a tela mostrar o próximo recurso elegível e a seleção já o assumir, a busca precisa ser redesenhada com a Zion para não consumir recursos durante a varredura.

**Conclusão:** reaproveitar o módulo de conexões e a infraestrutura Telnet, sim; reaproveitar um módulo ScanTE de “recursos operacionais”, não foi possível porque tal módulo não foi localizado. Viabilidade condicionada à interface/documentação e à confirmação do Zion sobre consulta e aquisição concorrente.

## 1. Auditoria do ScanTE

### Arquivos e componentes relevantes

| Componente | Papel comprovado no código |
|---|---|
| `EmuladorTelnet/app/src/main/java/com/logisticapp/emuladortelnet/database/DatabaseEntities.kt` | `SavedConnection` descreve uma conexão (nome, host, porta, credenciais e opções); `SessionLog` descreve uma sessão Telnet. Não contém entidade de tarefa/recurso do WMS. |
| `.../database/TelnetRepository.kt` | Persiste conexões e logs como JSON em `SharedPreferences` (`telnet_hosts`, `telnet_session_log`) e expõe fluxos locais. Não é Room, servidor ou integração com Zion. |
| `.../HostsActivity.kt`, `.../HostConfigActivity.kt`, `.../ui/HostsViewModel.kt` | Interface e lógica para listar/configurar/conectar a hosts salvos. |
| `.../SessionStore.kt` | Mantém sessões Telnet em memória no dispositivo (slots até 8); estado CONNECTED/CONNECTING é estado de conexão do emulador, não disponibilidade de trabalho no WMS. |
| `.../network/TelnetClient.kt` | Cliente Telnet por socket TCP, com negociação Telnet e opções de conexão; não implementa API Zion. |
| `.../ui/TelnetViewModel.kt`, `.../terminal/TerminalEmulator.kt` | Recebe bytes/texto, alimenta emulador de terminal, envia teclas e comandos à sessão conectada. O texto recebido é apresentado ao operador; não há parser de catálogo/status de recurso Zion. |
| `.../terminal/ScreenRecorder.kt`, `docs/GRAVADOR-DE-TELAS.md` | Captura snapshots/telas e eventos para futura adaptação de telas. É gravação, não monitoramento de disponibilidade nem mecanismo de seleção. |

### Significado e regras atuais de status

- “Conexão salva” significa somente host/porta configurados localmente.
- CONNECTING/CONNECTED/DISCONNECTED/ERROR refletem o estado da sessão de rede do emulador.
- `SessionLog.isActive` deriva de `endTime == 0`; é histórico local de conexão.
- Não há status “recurso disponível/ocupado” originado no Zion, atualização periódica, sincronização com coletor nem associação entre um recurso do WMS e uma sessão.
- Existem APIs do próprio ScanTE para licença/dispositivo (`LicenseApiService`), sem relação encontrada com tarefas Zion.
- `AppDatabase.kt`, `DatabaseDao.kt` e `DatabaseEntities.kt` registram que Room foi removido; persistência atual de hosts/sessões é local via `SharedPreferences`.

## 2. Evidência pública sobre Zion e Telnet

A página pública da Zion Logtec informa integração do Zion WMS por EDI, Oracle, TXT, XML e Webservice, citando REST, SOAP e JSON para comunicação com ERP e layouts parametrizáveis. A mesma página divulga uso de coletores RF e acesso a dados operacionais em tempo real. Isso comprova que existem capacidades gerais de integração no produto, mas **não** documenta endpoint, esquema, autenticação, disponibilidade de tarefas, interface externa para selecionar tarefa ou compatibilidade específica com o ScanTE. [Página Zion WMS — integrações](https://solucoeszion.zionlogtec.com.br/zionwms/)

A página atual do produto descreve gestão de estoque, integração com coletores RF e visibilidade por dashboards/relatórios/consultas online. Também não especifica uma API pública de recursos operacionais. [Zion WMS — página do produto](https://zionlogtec.com.br/zion-wms/)

Telnet padroniza transporte terminal bidirecional sobre TCP com bytes de controle e opções negociadas; a RFC não define o catálogo, estado ou semântica de tarefas de um aplicativo WMS. Portanto, descobrir telas pelo stream é dependente da implementação e parametrização concretas do Zion/cliente. [RFC 854 — Telnet Protocol Specification](https://www.rfc-editor.org/rfc/rfc854)

### O que não foi comprovado

- Uma API Zion publicada/documentada para listar tarefas ou postos disponíveis.
- Endpoint REST/SOAP, view Oracle, tabela, arquivo de integração, licença ou módulo contratual que exponha esse estado.
- O significado operacional da palavra “recurso” usada pelo cliente (pode ser tarefa, onda, doca, fila, equipamento ou outro objeto).
- Se selecionar um item via API faz claim/lock/atribuição atômica, ou se outro operador ainda pode obtê-lo em paralelo.
- Quais são os estados oficiais do Zion, sua latência, permissões por operador e mensagens em falha.
- O servidor, versão, configuração de terminal, telas e sequência de navegação da instalação do cliente.

Não foi localizada documentação técnica pública específica sobre esses pontos. A ausência nos resultados públicos não prova que o fornecedor não possua API ou integração privada.

## 3. Alternativas técnicas

| Abordagem | Benefícios | Limites e riscos | Pré-requisitos |
|---|---|---|---|
| API/serviço oficial Zion para consultar e assumir recurso | Semântica de negócio explícita; permite consulta atual e confirmação atômica na seleção; melhor alternativa para status confiável. | Pode não existir no contrato/versão; autenticação, limites e operação precisam ser desenhados. | Documentação e suporte Zion; escopos, ambientes, IDs/status e operação de claim/seleção transacional. |
| Integração autorizada (Webservice REST/SOAP, Oracle, arquivo/EDI) | A página do fornecedor confirma genericamente essas famílias de integração. Pode servir para catálogo ou consulta. | Essa informação não prova que esse fluxo/status esteja exposto. Arquivos/replicação podem estar defasados; acesso direto ao banco pode não ser suportado e pode contornar regras. | Confirmação do fornecedor sobre interface e suporte específico, consistência/latência, credenciais e rede. Preferir serviço a leitura direta do banco. |
| Interpretar telas/respostas Telnet | Aproveita a sessão operacional e o que o host já entrega ao operador; evita uma segunda fonte se a seleção continuar no fluxo original. | Frágil a alterações de tela, menus, terminal, idioma e perfil; texto visível não garante estado atual nem claim atômico; pode exigir manter tela/fluxo compatível com operador. | Sessões/telas reais autorizadas, sequência determinística, homologação por versão e fluxo de seleção confirmado pelo Zion. |
| Adaptador no emulador (intermediar/observar stream) | Pode projetar uma lista no coletor mantendo canal existente; aproveita `TelnetViewModel` e parser/gravador. | Hoje o app é cliente direto; intermediação exige novo parser/estado e pode alterar o timing/semântica do terminal. Concorrência continua sendo problema se a seleção não for confirmada pelo host. | Definir protocolo e máquina de estados, identificar limites da sessão, teste em aparelhos e fallback transparente para terminal. |
| Exportação de relatório/dashboard/consulta online | Pode ser rápida para exploração ou protótipo de leitura. | Pode ser somente leitura, permissão de gestor, intervalo de atualização ou sem mecanismo para adquirir recurso. Não é garantia operacional. | Validar se há API/export, frequência e papel do usuário; não chamar “disponível” se informação for snapshot antigo. |

## 4. Arquitetura recomendada (condicionada à confirmação)

1. **Fonte de verdade Zion:** serviço suportado fornece lista contextualizada (depósito/área/tipo de trabalho/usuário) com ID estável, descrição e estado/documentado timestamp/versão.
2. **Adaptador WMS isolado:** um contrato no ScanTE converte respostas Zion em estados conhecidos; mantém “desconhecido/erro/desatualizado” distinto de “indisponível”. Credenciais e permissões devem seguir mecanismo aprovado pelo fornecedor. Não gravar dados sensíveis em logs nem copiar senhas Telnet para outro serviço.
3. **Seletor no coletor:** lista compacta, acessível por toque e teclado/leitor, com ID, tipo, estado reconhecido e hora de atualização. Filtrar disponíveis sem esconder falha ou obsolescência da consulta.
4. **Aquisição na seleção:** ao tocar/escanear, fazer operação de claim/seleção no Zion e só avançar quando Zion confirmar. Em conflito, atualizar a lista e informar que o item já foi assumido. Se Zion só oferece consulta, deixar claro que há corrida possível e preservar o passo de confirmação dentro da tela/fluxo normal.
5. **Falha segura:** timeout, desconexão, resposta inesperada, falta de permissão ou dados vencidos não devem ser exibidos como “livre”. Mostrar erro/estado não confirmado e permitir retornar ao terminal atual sem perder a sessão.
6. **Sem reserva local paralela:** nenhum lock local no ScanTE; cache somente para exibição temporária, carimbado com idade e invalidado após seleção/falha.

Se o acesso ao WMS existir somente via terminal, avaliar uma etapa intermediária: detectar uma tela identificada por assinatura robusta, apresentar itens que a sessão autenticada recebeu e enviar a escolha pela sequência documentada de teclas/comandos; então aguardar resposta explícita de sucesso/concorrência do Zion. O terminal cru permanece disponível como contingência. Não inferir que cor, cursor ou mensagem isolada equivalem a status sem validar com o cliente/fornecedor.

## 5. Plano incremental de implementação (após aprovação e respostas externas)

1. **Descoberta/homologação:** capturar fluxo Telnet com conta de teste; obter terminologia, mapa de telas/API, estados e regras de seleção; acordar IDs e critérios de aceitação.
2. **Prova técnica somente leitura:** consultar fonte autorizada e validar mapeamento/frescor/erros; não adicionar botão de claim até o contrato estar definido.
3. **Contrato e adaptador:** criar modelo próprio de tarefa Zion separado de `SavedConnection`; implementar autenticação, timeout, parser/cliente conforme interface aprovada, métricas sem dados pessoais.
4. **UI no coletor:** adicionar entrada contextual ao fluxo de host/sessão e lista compatível com resolução/touch/teclas do hardware; preservar sessão e atalho ao terminal.
5. **Seleção transacional e concorrência:** implementar confirmação de claim/resultado Zion e estados de conflito; sem lock do lado ScanTE. Para a alternativa Telnet, manter a varredura não destrutiva, cancelável, limitada e validada a cada transição, com retorno/fallback seguro.
6. **Piloto e rollout:** homologar em ambiente não produtivo, testar nos modelos de coletor usados pelo cliente, habilitar por configuração/empresa e observar métricas antes de liberar.

## 6. Plano de validação

- Lista vazia, muitos itens, IDs/descrições longas, caracteres acentuados e paginação/filtros.
- Item livre no início, ocupado/indisponível e estados desconhecidos; validar o mapeamento junto à Zion.
- Dois coletores selecionam o último item simultaneamente: apenas a resposta confirmada pelo Zion prossegue; o perdedor recebe conflito e atualização.
- Item muda de estado entre listagem e toque; seleção deve revalidar/claim no WMS.
- Latência, timeout, perda de Wi-Fi, servidor fora, HTTP/Telnet interrompido, resposta malformada e retomada de sessão; nunca converter falha em “disponível”.
- Permissões insuficientes, sessão expirada e segregação por depósito/área/usuário.
- Compatibilidade de fluxo atual: login, navegação Telnet, teclas especiais, leitor de código de barras, retorno ao terminal, reconexão e hosts paralelos.
- Usabilidade nos coletores físicos do cliente: tamanho de fonte, navegação física, toque, rotação/resolução e desempenho.
- Se for parser de tela: variações por terminal/versão, telas parciais, ANSI/VT, redimensionamento, atualização de campo, cursor e detecção de sucesso/erro.
- Navegação automatizada: cancelamento, timeout sem envio tardio de comandos, prompt/modal inesperado, varredura incompleta, retorno à tela inicial e confirmação de que percorrer itens não os consome/assume.
- Concorrência entre leitura e seleção: outro operador altera o estado durante a varredura, entre exibição e toque, e durante a confirmação final; apresentar o resultado do Zion em cada caso.

## 7. Perguntas pendentes para cliente e fornecedor

1. Qual objeto é chamado de “recurso” e como o operador identifica hoje o próximo item? Informar nomes de menu, códigos/status e regras (área, operação, turno, perfil).
2. Qual produto/versão exata do Zion e qual host/canal o coletor utiliza? Telnet puro, SSH, gateway ou MLW Mobile? Quais opções/terminal negociados?
3. A Zion oferece API/serviço para listar e assumir o recurso? Pedir documentação OpenAPI/WSDL, exemplos, autenticação, disponibilidade no contrato e ambiente de homologação.
4. Se houver integração Oracle/EDI/arquivo, quais objetos são oficialmente suportados para consulta operacional? Qual SLA de atualização? A leitura dá status atual ou snapshot?
5. Qual operação confirma que o recurso foi tomado? É atômica/idempotente? Como dois operadores concorrentes recebem conflito?
6. Quais estados existem e quais transições são válidas? Existe expiração/liberação automática e qual usuário/ação altera o estado?
7. Como filtrar recursos elegíveis por usuário, filial, depósito, área e tipo de operação? Quais permissões e auditoria são necessárias?
8. O fornecedor autoriza integração por interpretação da tela Telnet? Há riscos ou restrições contratuais/técnicas?
9. Quais coletores, resoluções, versões Android e perfis de operador devem ser homologados? Há conta e massa de teste sem impacto na operação?

## Fontes

- Repositório ScanTE: código Android e documentação listados na seção de auditoria (revisados em 23/10/2026).
- [Zion WMS — página pública da Zion Logtec com menção a EDI, Oracle, TXT/XML e Webservice REST/SOAP/JSON](https://solucoeszion.zionlogtec.com.br/zionwms/).
- [Zion WMS — página atual do produto](https://zionlogtec.com.br/zion-wms/).
- [RFC 854 — especificação do protocolo Telnet, RFC Editor](https://www.rfc-editor.org/rfc/rfc854).
