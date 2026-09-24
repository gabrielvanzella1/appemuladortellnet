# Pendências — Centauro

| # | Pendência | Bloqueia | Dono | Prazo |
|---|---|---|---|---|
| P-01 | Endereço/porta do host Telnet da Centauro e um usuário de teste | EMU-04, EMU-05 | Centauro | Apresentação |
| P-02 | Um Zebra MC33 disponível para testar o Monitor (o caso real, sem GPS) | MON-06 e a repetição de todos os OK-EMULADOR | Centauro / parceiro | Apresentação |
| P-03 | Lista dos roteadores Wi-Fi do CD e nome de cada zona (ou deixar os roteadores aparecerem no painel e nomear lá) | PAN-03, PAN-06 | Centauro | Após instalar o Monitor |
| P-04 | Confirmar se a Centauro usa sistema web no coletor (Browser), host fora do CD (Relay) ou impressora (ESC/POS) | Escopo | Centauro | Apresentação |
| P-05 | "Sem restrição de bateria" ficou ✗ no emulador — conferir no MC33; se o Android matar o serviço, o coletor aparece "sem comunicação" | MON-04 | ScanTE | Teste no MC33 |
| P-06 | ~~Testar trocar senha, desativar e fila sem internet~~ — feito no emulador em 24/09; repetir no MC33 junto com P-02 | — | ScanTE | Feito (emulador) |
| P-07 | O funcionário consegue forçar parada ou desinstalar o Monitor pelo Android. Solução: modo Device Owner [PLANEJADO] | Segurança da frota | ScanTE | Pós-apresentação |
| P-08 | Licenciamento do Monitor: hoje ele usa uma licença ScanTE da empresa. No coletor que também roda o Emulador, é a mesma licença; num coletor só com o Monitor, consome uma licença. Definir se vira produto próprio | Comercial | Gabriel | Pós-apresentação |
| P-09 | A tela do Monitor diz "monitoramento desativado para esta empresa" também quando a licença foi desvinculada (mensagem enganosa) | Suporte | ScanTE | Pós-apresentação |
| P-10 | [SEGREDO ENCONTRADO — NÃO EXPOR] Segredo da API fixo no código dos apps. Solução: token por dispositivo emitido na ativação | Segurança | ScanTE | Pós-apresentação |
| P-11 | Código atual ainda não enviado ao GitHub (branch `unificacao`, fora da pasta principal) | Rastreabilidade | Gabriel | Hoje |
| P-12 | Apagar os dados simulados (SIM-*) da empresa "Scan TE Produção" depois da apresentação (`seed_cerca_demo.php --empresa=4 --limpar`) | Limpeza | ScanTE | Pós-apresentação |
| P-13 | Implantar em ~500 MC33 sem MDM exige instalar, ativar (digitar chave) e dar 5 permissões em cada aparelho. Avaliar instalação em massa (ex.: Zebra StageNow) e ativação sem digitar chave [NAO CONFIRMADO] | Implantação em escala | ScanTE | Pós-apresentação |
| P-14 | `scripts/preparar-demo-cerca.ps1` aponta para a pasta `emulador-unificacao`; ajustar quando o código for unificado | Manutenção | ScanTE | Após o push |
