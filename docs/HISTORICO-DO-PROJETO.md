# Histórico do projeto

Resumo cronológico do que aconteceu no ScanTE (antigo "Emulador Telnet"), montado a
partir do histórico do git e dos docs do repositório. Serve pra lembrar rápido de onde
veio cada decisão sem precisar vasculhar commit por commit. Datas são as dos commits —
o trabalho de um dia às vezes aparece com data de outro por causa de como foi versionado
ao longo do tempo (nomes como "23-06" ou "28-07casa" são commits antigos, de antes do
projeto ter uma rotina de mensagens descritivas).

Onde não há certeza (porque não achei registro, só o relato de alguém), está marcado
como tal — evita tratar lembrança como fato confirmado.

## Maio 2026 — o MVP

Em 21/05 nasce o projeto: um emulador de terminal Telnet pra Android, com socket TCP
real (RFC 854), parser ANSI/VT100 com cores, histórico de comandos, banco local (Room)
pra salvar conexões e sessões, e um primeiro sistema de licença com fingerprint de
dispositivo. Esse já é o núcleo que existe até hoje.

## Junho–julho 2026 — licenciamento e infraestrutura

Em 30/06 entra o fluxo de licença ligado ao scante-admin (o painel administrativo) e um
painel de debug pra testar ativação. Em 06/07 esse fluxo vira obrigatório — passa a
exigir ativação por chave, não dá mais pra usar o app sem licença.

Em 03/07 nasce o **ScanTE Relay**: um programa que roda no computador da empresa e
mantém a sessão do coletor viva mesmo quando o Wi-Fi cai por alguns instantes.

Em 13/07, os três projetos que antes viviam separados — o app Emulador, o scante-admin
e o scante-relay — são unificados num repositório só. Esse é o repositório onde este
arquivo está.

O resto de julho é ajuste de usabilidade: pinça pra redimensionar o terminal, mais
tamanhos de fonte, botões de Backspace e Ctrl (A-Z) na barra de ferramentas.

## Agosto 2026 — sandbox e produção

Em 11/08 acontece uma mudança estrutural importante: o app e o admin passam a ter
ambiente **sandbox** separado do de **produção** (flavors no Android, config separada no
PHP). A partir daqui dá pra testar mudança arriscada sem afetar clientes reais — é esse
mecanismo que permite todo o trabalho de prototipagem mais recente (ver seção de
outubro).

## 17–18 de setembro — ajustes pré-lançamento

Correção de um bug sério: o leitor de código de barras falhava ao registrar em Android
13+. Também entra o modo Browser (WebView) como alternativa ao Telnet puro, a assinatura
de release por keystore.properties, e a correção do segredo de API que estava
divergente entre app e produção (bloqueava ativação).

## 22–24 de setembro — a grande unificação e o nascimento do Monitor

Esses três dias concentram bastante coisa:

- O ScanTE Browser e o ScanTE Config viram apps separados do Emulador.
- O repositório "de casa" (`EmuladorCompleto`) é unificado com o do trabalho.
- Sai a versão **1.1.0** (versionCode 2) — primeiro app realmente unificado.
- O modo Browser e o pagamento pelo próprio app saem do Emulador (venda passa a ser por
  transferência).
- Nasce a **cerca digital** (rastreamento por Wi-Fi do coletor, sem GPS) — primeiro
  dentro do próprio Emulador, depois (ainda em 24/09) é retirada de lá e vira a base do
  novo app **ScanTE Monitor 1.0.0**.
- Entra o bloqueio de edição de conexão por usuário/senha da empresa.
- Sai a versão **1.1.1** (versionCode 4), já chamada de "versão Centauro" no commit —
  marca o início do ciclo de homologação com esse cliente
  (`docs/homologacao/centauro/`).
- É documentado que o segredo da API já estava público no GitHub desde 18/09 (pendência
  de segurança, resolvida depois).

## 23–25 de setembro — primeira visita à Centauro (comprovada)

A única visita à Centauro com registro real no banco de produção: o coletor **Honeywell**
(nome salvo como `25351B041D`) é vinculado em 23/09 às 15:42, e revinculado em 25/09 às
13:28 com um `device_id` novo — provavelmente porque o app foi reinstalado sem a
assinatura oficial (a keystore certa só chegou na máquina de casa depois, em outubro).
Último acesso registrado desse aparelho: **25/09/2026 13:28**. Não há nada depois disso
no banco.

Relato do usuário (21/10/2026, não verificado em banco/git): essa primeira visita foi
feita junto com o Gabriel, no coletor Honeywell. Uma segunda visita, mais recente, foi
feita só pelo **Fabrício** (sócio), já com um coletor **Urovo** — esse segundo episódio
não tem nenhum registro encontrado até agora (nem git, nem no que foi conferido do
painel).

## 1 de outubro — ajustes específicos pra Centauro

Depois da primeira visita, um commit batiza três correções como "(Centauro)":

- **Reconexão após erro**: o estado ERROR passa a contar como sessão morta (igual
  DISCONNECTED), senão o app travava na tela de erro sem jeito de reconectar.
- **Teclado físico do coletor**: `onKeyDown` em `TerminalView` passa a cobrir o teclado
  físico (que não passa pelo IME do Android).
- **NEL**: o `TerminalEmulator` passa a tratar a sequência de controle VT100 `ESC E`
  (Next Line) — se não tratada, ela pode fazer o texto desalinhar/sobrepor na tela. É o
  candidato mais forte pra explicar um relato do usuário de que "estava tudo
  embaralhado" antes desse ajuste — mas como o último acesso real do Honeywell é de
  25/09 e esse commit é de 01/10, esse fix **nunca foi confirmado rodando no aparelho
  real da Centauro**, só testado localmente.

No mesmo dia: o fluxo de pagamento online (Pagar.me + Mercado Pago) é removido por
completo do admin — vendas passam a ser só por transferência — e uma falha de segurança
no webhook do Pagar.me é corrigida antes da remoção total.

## 14 de outubro — multi-host desbloqueado

Sessões novas só travam host/porta se a empresa tiver pedido bloqueio de conexão no
painel. A Centauro está com esse bloqueio desligado, então já consegue cadastrar e
alternar entre hosts diferentes.

## 20 de outubro — alarme, atualização automática, segurança de build

- Som de alerta personalizável no alarme do host (BEL) — toca no volume de alarme, não
  no de notificação, de propósito (ignora "Não perturbe" sem precisar de permissão
  especial). Documentado como **suposição não testada**: assume que o host da Centauro
  manda BEL (0x07) em código inválido, baseado no que o usuário relatou, não em teste
  contra o host real.
- App passa a verificar sozinho se existe uma versão mais nova (por empresa), com link
  de download protegido por token aleatório no nome do arquivo (achado de segurança:
  sem isso, dava pra adivinhar o nome de um build exclusivo de empresa).
- Notas sobre a chave oficial de assinatura e como assinar localmente.

## 21 de outubro — correção de XSS e início do trabalho de reformatação de tela

Corrige um XSS no seletor de versão do app no painel admin (nome da empresa não
escapado).

A partir daqui começa o trabalho descrito no restante deste histórico, ainda em
andamento numa branch separada (`feature/reformatacao-tela-poc`, nunca mesclada na
`main`):

- Pesquisa sobre como concorrentes (Ivanti Velocity, JLT Freefloat Access) reformatam
  telas de terminal legado em UI gráfica moderna — e confirmação de que o ScanTE já
  cobre parte do padrão (auto-advance do scan, detecção automática de campo por vídeo
  reverso).
- Identificado um risco técnico ainda não resolvido: se o sistema da Centauro for
  AS/400/IBM i de verdade (protocolo 5250, tela inteira por vez), o ScanTE — que é um
  emulador VT100/ANSI, caractere a caractere — pode não conseguir renderizar certo. Seguia
  sem confirmação em 21/10 (ver `docs/homologacao/centauro/pendencias.md`, P-04).
- Construído um servidor telnet de demonstração (`scripts/demo_telnet_server.py`,
  **não é o ERP real da Centauro**) simulando uma tela de recebimento de mercadoria.
- Construído, testado no coletor Honeywell EDA52 físico, um protótipo de reformatação
  de tela: reconhece a tela de demonstração e troca o grid cru por um formulário nativo
  (campos reais, botão de confirmar), com cor e logo extraídos do site oficial da
  Centauro (vermelho aproximado por amostra de pixel, não é o hex do manual de marca).
  Arquitetura em "modelos de tela" (`ReformatTemplate`): tela nova = nova entrada numa
  lista, não reescrita de código.

## O que ainda está em aberto

- Confirmar com a Centauro se o sistema deles é telnet/VT220 (como o Protheus costuma
  expor) ou 5250 de verdade — decide se a abordagem atual funciona sem mudança nenhuma.
- Nunca foi testado, no aparelho real da Centauro, o fix de NEL de 01/10.
- O painel de debug de licença (`LicenseActivity.kt`) está religado temporariamente pra
  testes — precisa ser desligado antes de qualquer build que saia dessa branch.
- A visita com o coletor Urovo (relatada pelo usuário) não tem registro confirmado em
  nenhum lugar — vale conferir a aba Dispositivos do painel.
