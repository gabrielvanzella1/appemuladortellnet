# Em andamento

Estado atual do trabalho — pra retomar de qualquer computador sem precisar perguntar
"onde eu parei". Atualizar isso sempre que parar no meio de algo ou terminar uma etapa,
não só no fim do dia.

---

## Agora: protótipo de reformatação de tela pra apresentação da Centauro (sexta, 24/10/2026)

**Branch:** `feature/reformatacao-tela-poc` (separada da `main` de propósito — é só pra
demonstrar, nunca vai pro build oficial que a Centauro recebe).

**Último commit:** `4c1850c` — "PoC de reformatação de tela pra Centauro + histórico e
memória do projeto". Local, ainda não deu `git push`.

### Feito
- [x] Servidor telnet de demonstração (`scripts/demo_telnet_server.py`) — simula uma
      tela "RECEBIMENTO DE MERCADORIA" em VT100 puro (cursor, vídeo reverso, BEL).
      **Não é o sistema real da Centauro.**
- [x] Overlay nativo no app (`MainActivity.kt`) que reconhece essa tela de demo e troca
      o grid cru por formulário Android (campos + botão), em `ReformatTemplate` — tela
      nova = entrada nova na lista `reformatTemplates`, não reescrita de código.
- [x] Branding: logo (`drawable-nodpi/centauro_logo.png`, recorte do site oficial) +
      vermelho aproximado (`rgb(227,0,0)`, amostra de pixel do site, não é hex de
      manual de marca).
- [x] Layout responsivo (overlay ocupa a tela toda, não só a largura do grid de 80
      colunas).
- [x] Testado de ponta a ponta no Honeywell EDA52 físico (digitar nos 3 campos, avançar
      com Enter, confirmar com F1 → "RECEBIDO COM SUCESSO"; campo faltando → BEL).
- [x] `docs/HISTORICO-DO-PROJETO.md` — cronologia completa do projeto.
- [x] Memória de projeto via Serena (`.serena/memories/`) — sobrevive entre máquinas
      porque está commitada.

### Não feito / próximo passo
- [ ] **Desligar o painel de debug de ativação** em `LicenseActivity.kt`
      (`if (BuildConfig.DEBUG)` → voltar pra `if (false && BuildConfig.DEBUG)`) antes
      de qualquer build que saia dessa branch. Está ligado agora só pra eu conseguir
      testar sem ativar por e-mail.
- [ ] Decidir se cadastramos uma segunda tela de demonstração (prova de que o padrão
      `ReformatTemplate` realmente generaliza, não só uma tela isolada).
- [ ] Confirmar com a Centauro: telnet/VT220 ou 5250 de verdade (P-04) — decide se a
      abordagem atual funciona sem mudança nenhuma. Ver `docs/homologacao/centauro/pendencias.md`.
- [ ] Testar o fix de NEL (01/10) no aparelho real — nunca foi confirmado lá.
- [ ] Confirmar a visita com o coletor Urovo (relatada, não tem registro) — checar aba
      Dispositivos do painel.
- [ ] `git push` quando quiser sincronizar com o outro computador.

### Como retomar (comandos)
```powershell
cd EmuladorTelnet
.\gradlew.bat assembleSandboxDebug
adb install -r app\build\outputs\apk\sandbox\debug\app-sandbox-debug.apk
```
No app: Sessões → "Demo PoC" (se a sessão ainda existir) → conectar. Depois, no
dispositivo: tecla Menu (ou `adb shell input keyevent 82`) → "Modo reformatado (PoC)".
Servidor de demonstração: `python scripts/demo_telnet_server.py 2323` (na máquina que
estiver na mesma rede Wi-Fi do coletor).

---

## Histórico de "em andamento" anteriores

(Quando essa seção ficar grande, mover os itens concluídos antigos pra
`docs/HISTORICO-DO-PROJETO.md` e deixar só o que está ativo aqui.)
