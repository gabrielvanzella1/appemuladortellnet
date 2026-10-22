# Como gravar as telas na Centauro (ScanTE 1.1.3)

Objetivo: capturar as telas reais do SHARK para montar a "tela gráfica". Leva ~20 min.

## Antes de sair (checklist)
- [ ] APK `ScanTE-1.1.3-gravador-20261022.apk` no coletor Urovo (copiar por cabo ou Drive).
- [ ] **1 licença trial livre da Centauro** anotada (painel da Centauro → Minhas Licenças). A do CK62 não serve.
- [ ] **IP e porta do SHARK** (perguntar ao TI da Centauro) e **um usuário de TESTE** do SHARK.
- [ ] Coletor com **bateria cheia** e Wi-Fi da Centauro conectado.
- [ ] Um jeito de tirar o arquivo do coletor: WhatsApp, Gmail ou Drive no aparelho; ou cabo USB no notebook.

## Instalar (Urovo — instalação nova)
1. Instalar o APK (permitir "fontes desconhecidas").
2. Ativar com a licença trial da Centauro.
3. Cadastrar o host: **+** → nome, IP e porta do SHARK → Conectar.

## Gravar
1. Conectar no SHARK uma vez (ou iniciar a gravação antes de conectar, para pegar também a tela de login).
2. Ir em **Configurações → Gravação de telas** → **Iniciar gravação**.
3. Voltar para a sessão. Aparece **● REC** vermelho no topo (tocar nele reabre a tela da gravação).
4. Fazer o trabalho normal, **passando por todos os fluxos** — marque ao concluir:
   - [ ] Login (certo)  · [ ] Login com senha errada
   - [ ] Menu principal e cada submenu
   - [ ] Recebimento · [ ] Separação · [ ] Conferência · [ ] Inventário · [ ] Endereçamento
   - [ ] **Erros de propósito:** código inexistente · quantidade inválida · campo vazio · endereço errado · produto já conferido
   - [ ] Telas de **lista** (que rolam) e telas de **confirmação/sucesso**
   - [ ] Tela de ajuda / mensagens do sistema, se houver
5. **Intervalo?** Configurações → Gravação de telas → **Pausar** (o selo fica amarelo ⏸). **Retomar** continua do mesmo ponto.
6. Ao terminar: Configurações → Gravação de telas → **Parar e enviar**. O app salva o arquivo, **copia para a pasta Downloads**
   do coletor (Arquivos → Downloads → `gravacao-….json`) e abre o **Compartilhar** (WhatsApp, e-mail, Drive).

## Se algo der errado (nada se perde)
- **O app grava o arquivo sozinho a cada poucos passos.** Se fechar sem querer, a bateria acabar ou o Wi-Fi cair, o que foi feito até ali está salvo.
- Para achar o arquivo: pasta **Downloads** do coletor, ou `Android/data/com.logisticapp.emuladortelnet/files/gravacoes/` (cabo USB, no notebook).
- Se o Wi-Fi cair, o app tenta reconectar sozinho e a gravação **continua**.

## Cuidados (importante)
- **NÃO é gravado o que é digitado:** só os nomes das teclas (Enter, F1…) e a quantidade de caracteres.
- **É gravado o que o SHARK mostra na tela** (usuário, códigos, quantidades; a senha só se o SHARK a mostrar sem `****`).
- Por isso: **usar usuário de teste** e **conferir o arquivo antes de enviar**.
- Se o SHARK tocar o bipe de erro, o arquivo registra `ALARME_HOST`: isso mostra se o sistema avisa o erro por som.

## Perguntas para anotar com o TI deles
1. É Telnet/VT220 puro ou 5250? (a gravação já vai confirmar)
2. O SHARK manda bipe quando o código está errado?
3. Quais são as 3 a 5 telas que o operador mais usa?
