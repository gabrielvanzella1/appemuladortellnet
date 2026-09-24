# ScanTE Monitor — Manual

App Android que fica ligado o tempo todo no coletor e informa ao painel da empresa:
- **em que roteador Wi-Fi** o coletor está (a zona do CD), inclusive em coletores sem GPS;
- a localização por GPS ou pela rede, quando o aparelho fornece;
- **bateria** e se está carregando.

Ele é separado do ScanTE (Emulador) e tem senha própria.

| Item | Valor |
|---|---|
| Versão | 1.0.0 (versionCode 1) |
| Android mínimo | 8.0 (API 26) |
| Pacote | `com.logisticapp.scantemonitor` |
| APK | `ScanTE-Monitor-1.0.0-20260924.apk` |
| Painel | ScanTE Admin → **Cerca digital** (precisa estar ligada para a empresa) |

Status das funcionalidades (conferido no código e testado no emulador em 24/09/2026):

| Funcionalidade | Status |
|---|---|
| Ativação com a chave de licença da empresa | [IMPLEMENTADO] |
| Serviço sempre ligado + início automático ao ligar o coletor | [IMPLEMENTADO] |
| Roteador Wi-Fi (BSSID/SSID/sinal), localização, bateria | [IMPLEMENTADO] |
| Leitura a cada 60 s; fila local de até 200 leituras sem internet | [IMPLEMENTADO] |
| Senha nas telas do app (padrão `1234`, trocável) | [IMPLEMENTADO] |
| Impedir desinstalar/forçar parada pelo Android (Device Owner) | [PLANEJADO] |
| Atribuir o coletor a um funcionário | [PLANEJADO] |

---

# Instalar e ativar

## Objetivo
Deixar o coletor aparecendo no painel da empresa, com o Monitor ligado sozinho.

## Pré-requisitos
- Coletor Android 8 ou superior, conectado ao Wi-Fi com internet.
- Chave de licença ScanTE da empresa.
  - No coletor que **já tem o ScanTE** instalado, use a **mesma chave** do ScanTE. Os dois apps têm a mesma assinatura: o painel vê um aparelho só e não gasta outra licença.
  - Num coletor **só com o Monitor**, a ativação usa uma licença da empresa.
- Cerca digital **ligada** para a empresa (feito pela ScanTE no painel de administração).

## Passo 1 — Instalar o APK
Copie o APK para o coletor (cabo USB, pendrive ou link) e toque nele. Se o Android pedir, permita "instalar apps desta fonte".

## Passo 2 — Ativar
Abra o **ScanTE Monitor**. Digite a chave (os hífens entram sozinhos) e o nome do coletor, por exemplo o número de série. Toque em **Ativar**.

## Passo 3 — Permissões
O app pede, nesta ordem. Aceite todas:
1. **Localização**: "Permitir durante o uso do app".
2. **Permitir o tempo todo**: o Android abre uma tela de configuração; escolha "Permitir o tempo todo" e volte.
3. **Notificações**: Permitir.
4. **Localização do Android ligada**: se estiver desligada, o app leva até a configuração.
5. **Sem restrição de bateria**: Permitir.

> Sem a permissão de localização, o Android esconde o roteador Wi-Fi (aparece `02:00:00:00:00:00`) e a cerca não funciona.

## Passo 4 — Trocar a senha padrão
Toque em **Trocar senha do coletor** e defina uma senha de 4 a 8 dígitos. Guarde-a: sem ela ninguém entra nas telas do app.

## Resultado esperado
A tela de status mostra 5 ✓, a leitura com o roteador Wi-Fi e a bateria, e o "Último envio ao painel" com horário.
Em 1 ou 2 minutos, o roteador aparece no painel em **Cerca digital → Pontos de acesso**.

---

# Configurar a cerca no painel

## Objetivo
Dizer quais roteadores ficam dentro do CD e dar nome às zonas.

## Passo 1
Painel da empresa → **Cerca digital → Pontos de acesso**. Os roteadores em que os coletores já se conectaram aparecem sozinhos.

## Passo 2
Marque **Na cerca** nos roteadores do CD e escreva o **nome da zona** de cada um (ex.: Recebimento, Doca 1). Toque em **Salvar cerca**.

## Passo 3
Em **Configurações**, ajuste se precisar:

| Configuração | Padrão |
|---|---|
| Alerta "sem comunicação" | 15 min sem sinal |
| Alerta de bateria baixa | 15% (só quando não está carregando) |
| Expediente | 06:00–22:00, segunda a sábado |

Fora do expediente, coletor calado aparece como "fora do expediente", não como alerta.

## Resultado esperado
Em **Visão geral**, cada coletor aparece na sua zona, como Dentro, Fora da cerca ou Sem comunicação, com a bateria. **Alertas** lista os problemas abertos. **Linha do tempo** mostra por onde o coletor passou no dia.

| Alerta | Abre quando | Fecha quando |
|---|---|---|
| Fora da cerca | O coletor se conecta a um roteador não marcado "na cerca" | Volta a um roteador da cerca |
| Sem comunicação | Fica mais que o limite sem sinal, dentro do expediente | Volta a mandar sinal |
| Bateria baixa | Abaixo do limite e sem carregar | Carrega ou sobe acima do limite + 5% |

---

# Senha do coletor

- Toda vez que o app é aberto, ele pede a senha (padrão `1234`). Senha errada mostra "Senha incorreta".
- Com a senha, o responsável pode: revisar permissões, enviar agora, trocar a senha e **desativar o monitoramento**.
- **Desativar** para o serviço e volta à tela de ativação. O coletor para de mandar sinal.
- **Limitação:** a senha protege as telas do app, mas **não impede** forçar a parada ou desinstalar pelas configurações do Android. Isso exige o modo Device Owner. [PLANEJADO]

---

# Problemas comuns

| Problema | Causa provável | Solução |
|---|---|---|
| "Licença vinculada a outro dispositivo" | A chave já está em outro aparelho | Use outra chave, ou peça à ScanTE para liberar a licença |
| "Licença não encontrada" | Chave digitada errada | Confira os 22 caracteres (sem contar os hífens) |
| Status mostra "Wi-Fi: não informado" | Sem permissão de localização, ou Wi-Fi desligado | Tocar em **Revisar permissões**; ligar o Wi-Fi |
| "Painel: monitoramento desativado para esta empresa" | Cerca desligada para a empresa, ou licença não vinculada | Falar com a ScanTE |
| Coletor aparece "sem comunicação" ligado | O Android pausou o app (restrição de bateria) ou não há internet | Permitir "sem restrição de bateria"; conferir a internet. As leituras ficam guardadas e chegam quando a internet volta |
| Roteador não aparece em Pontos de acesso | O Monitor ainda não mandou sinal por aquele roteador | Aguardar 1–2 min ou tocar em **Enviar agora** |
| Esqueceu a senha do coletor | — | Reinstalar o app (apaga a senha e a ativação) e ativar de novo |

---

# Referência técnica (resumo)

| Item | Valor | Fonte |
|---|---|---|
| Leitura | a cada 60 s; 5 min se a cerca da empresa estiver desligada | `MonitorService.kt` |
| Re-registro no painel (ping) | a cada ~30 leituras | `MonitorService.kt` |
| Fila sem internet | até 200 leituras | `MonitorService.kt` |
| Envio | `POST /api/dispositivo/sinal` (lote) e `POST /api/dispositivo/ping` | `Api.kt`, `DispositivoController.php` |
| Identificação | ANDROID_ID; igual ao do ScanTE quando os dois APKs são release com a mesma chave | teste de 24/09 |
| Início automático | `BOOT_COMPLETED` e `MY_PACKAGE_REPLACED` | `AndroidManifest.xml` |
| Segredo da API | [SEGREDO ENCONTRADO — NÃO EXPOR]: fixo no app, trocar por token por dispositivo | `Api.kt` |
