# Centauro — status em aberto (atualizado 21/10/2026)

## Pendências críticas (docs/homologacao/centauro/pendencias.md)
- P-01: endereço/porta do host Telnet real da Centauro — NUNCA confirmado.
- P-04: que tipo de sistema é — telnet/VT220 (tipo Protheus) ou AS/400 5250 de
  verdade? Importa tecnicamente: o ScanTE é VT100/ANSI (caractere a caractere), não
  implementa 5250 (tela inteira). Se for 5250 genuíno, pode não funcionar sem mudança
  de abordagem. Ainda sem resposta.

## O que está comprovado vs. só relatado
- Comprovado no banco de produção: coletor Honeywell (`25351B041D`) vinculado
  23/09/2026, revinculado 25/09/2026 (troca de device_id, provável reinstalação sem
  assinatura oficial). Último acesso: 25/09/2026 13:28. Nada depois disso.
- Relatado pelo usuário, NÃO verificado em nenhum registro: uma segunda visita, só com
  o sócio Fabrício, usando um coletor Urovo. Checar aba Dispositivos do painel se
  precisar confirmar.
- Fix de NEL (ESC E, commit 01/10) é o candidato mais forte pra explicar uma tela
  "embaralhada" que o usuário lembrava, mas nunca foi testado no aparelho real (depois
  do último acesso registrado).

## Trabalho em andamento (branch feature/reformatacao-tela-poc, separada da main)
Protótipo de reformatação de tela (inspirado em Ivanti Velocity/JLT Freefloat): troca
o grid VT100 cru por um formulário nativo Android quando reconhece uma tela conhecida.
- Arquitetura em `ReformatTemplate` (texto-chave + lista de campos) dentro de
  `MainActivity.kt` — tela nova = nova entrada na lista `reformatTemplates`, não
  reescrita de lógica.
- Único template hoje: tela de demonstração do `scripts/demo_telnet_server.py` (NÃO é
  o ERP real da Centauro).
- Cor de marca: vermelho `rgb(227,0,0)` extraído por amostra de pixel do site
  centauro.com.br — aproximação, não é hex de manual de marca oficial.
- Logo: recorte do print do site, em `app/src/main/res/drawable-nodpi/centauro_logo.png`.
- Testado de ponta a ponta no Honeywell EDA52 físico via adb/mobile-mcp.

## Atenção antes de qualquer build que saia dessa branch
`LicenseActivity.kt` tem o painel de debug de ativação religado temporariamente
(`if (BuildConfig.DEBUG)` em vez de `if (false && BuildConfig.DEBUG)`) pra testes sem
precisar ativar por e-mail. **Desligar antes de mergear ou gerar build de release.**
