# Checklist de homologação — por coletor

Copie este bloco uma vez por coletor testado. Detalhes de cada item em [cenarios.md](cenarios.md).

```
Coletor: ______________  Modelo: ______________  Android: ____  Data: __/__/____
Testado por: ______________

EQUIPAMENTO
[ ] Modelo, número de série e versão do Android anotados
[ ] Coletor conectado ao Wi-Fi do CD (anotar SSID: __________)
[ ] Data/hora do coletor corretas (fuso America/Sao_Paulo)

SCANTE EMULADOR
[ ] APK instalado — versão exibida: ______ (esperado 1.1.0)        → EMU-01
[ ] Licença ativada e aparece no painel da empresa                 → EMU-02
[ ] Conecta no host Telnet da Centauro                             → EMU-04
[ ] Login automático funciona                                      → EMU-05
[ ] Leitura de código de barras chega no campo certo               → EMU-06
[ ] Teclado ScanTE / teclas de função respondem                    → EMU-07
[ ] Queda e volta do Wi-Fi testadas (anotar o que aconteceu)       → EMU-08

SCANTE MONITOR
[ ] APK instalado — versão exibida: ______ (esperado 1.0.0)        → MON-01
[ ] Ativado com a chave da empresa                                 → MON-02
[ ] Permissões: localização "o tempo todo", notificações, bateria  → MON-04
[ ] Sinal chegou no painel (roteador Wi-Fi + bateria)              → MON-05 / MON-06
[ ] Aparece como UM dispositivo junto com o Emulador               → MON-07
[ ] Reiniciar o coletor: Monitor volta sozinho                     → MON-08
[ ] Abre bloqueado; senha errada recusada; senha certa libera      → MON-09
[ ] Senha do coletor trocada da padrão (1234)                      → MON-10
[ ] Sem internet: sinais guardados e enviados na volta             → MON-13

PAINEL
[ ] Coletor listado com versão, licença e último acesso            → PAN-02
[ ] Roteador(es) do CD marcados "na cerca" com nome de zona        → PAN-03
[ ] Coletor aparece na zona certa                                  → PAN-03
[ ] Alertas conferidos (sem comunicação / bateria / fora da cerca) → PAN-04..06

ENCERRAMENTO
[ ] Evidências salvas (ver evidencias.md)
[ ] Pendências registradas (ver pendencias.md)
[ ] Resultado atualizado (ver resultado.md)
```
