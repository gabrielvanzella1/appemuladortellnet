# Continuação do trabalho

## Estado observado em 22/10/2026

- A branch aberta para a apresentação é `feature/reformatacao-tela-poc`.
- O commit `453547d` integra a branch remota `old-origin/feature/gravador-telas`, cujo último commit é `866356d`.
- O gravador e a PoC de reformatação estão juntos nesta branch local. O merge foi automático; depois, as notas de retomada e o roteiro de apresentação foram atualizados localmente.
- O merge ainda não foi enviado ao remoto. `assembleSandboxDebug` compilou e foi instalado no EDA52; a sessão conectou ao servidor simulado via `adb reverse` e mostrou automaticamente o formulário.
- O app Production já instalado no EDA52 foi preservado com a licença atual. O APK Production Release para atualização ainda depende da keystore privada; a autenticação GitHub CLI local está inválida.
- `origin` e `old-origin` são remotos distintos; não misturar `origin/main` nesta branch sem revisar o histórico e o objetivo.

## Conteúdo desta branch

- **Gravador:** captura telas/eventos Telnet, permite pausar/retomar/parar e exportar/copiar a gravação. Consulte [`docs/GRAVADOR-DE-TELAS.md`](docs/GRAVADOR-DE-TELAS.md).
- **PoC Centauro:** reconhece uma tela de demonstração específica e mostra um formulário Android. Não é integração pronta com o ERP/SHARK real.
- **Demonstração:** siga [`docs/homologacao/centauro/roteiro-demo-poc.md`](docs/homologacao/centauro/roteiro-demo-poc.md).

## Pontos para validar no aparelho

1. Reautenticar GitHub CLI, obter a keystore do repositório privado e preparar `EmuladorTelnet/keystore.properties` sem versionar credenciais.
2. Gerar `assembleProductionRelease`, confirmar o certificado oficial `c8eef2b7…` e instalar por cima do app Production existente para manter licença e dados.
3. No Sandbox instalado, a detecção automática foi confirmada. Se repetir o teste por USB, iniciar `scripts/demo_telnet_server.py 2323`, usar `adb reverse tcp:2323 tcp:2323` e host `127.0.0.1`; para a rede real, configurar host/porta fornecidos pelo TI.
4. Se houver sessão real autorizada, testar o gravador separadamente com usuário de teste e conferir o JSON antes de compartilhar.
5. Não distribuir build Debug como release; `LicenseActivity` mostra o desbloqueio de teste em builds Debug.

## Cuidados com o gravador

- Um único `ScreenRecorder.instance` atende a sessão em primeiro plano. Ao trocar para outro host, o gravador pausa para evitar misturar sessões.
- O texto digitado não é gravado diretamente, mas o que o host ecoa aparece nos snapshots. A máscara de rótulos de senha é melhor esforço; revisar o arquivo antes do envio.
- A implementação foi testada no emulador segundo a documentação; o ensaio no coletor físico continua pendente.

## Ao encerrar uma sessão de trabalho

Atualize este arquivo e `docs/EM-ANDAMENTO.md` com branch/commit, alterações, validações feitas, problemas e próximo passo. Confira `git status` antes de editar ou limpar arquivos.
