# Deploy na Hostinger — ScanTE Admin

Banco: `u508103998_scante` · Usuário: `u508103998_scante_user` · Domínio: `scante.com.br`

Pasta no servidor (atual): `public_html/scante-admin/`

---

## 1. Criar as tabelas (phpMyAdmin)

1. hPanel → **Bancos de dados** → **phpMyAdmin** no banco `u508103998_scante`.
2. Selecione o banco `u508103998_scante` na coluna da esquerda.
3. Aba **Importar** → escolha o arquivo **`database_hostinger.sql`** → **Executar**.

> Use `database_hostinger.sql` (NÃO o `database.sql`). O `database.sql` tem `CREATE DATABASE`/`USE`
> que não funcionam na Hostinger. O arquivo de produção cria as 7 tabelas e o usuário admin.

Ao final você deve ver as tabelas: `empresas, usuarios, licencas, historico_dispositivos,
pagamentos, configuracoes, dispositivos`.

## 2. Configurar a senha do banco

Edite **`config/config.php`** e troque a linha da senha pela senha real do usuário MySQL:

```php
define('DB_PASS', 'SUA_SENHA_REAL_AQUI');
```

Depois **re-envie o `config/config.php`** por FTP (ou edite direto no **Gerenciador de
Arquivos** do hPanel). Os demais valores (host `localhost`, nome e usuário do banco,
`API_SECRET`) já estão preenchidos.

## 3. Confirmar arquivos enviados

Garanta que estes também subiram por FTP (o FTP costuma ignorar arquivos que começam com ponto):
- `.htaccess` na raiz de `scante-admin/` (protege `config/`, `app/` e os `.sql`)
- `public/.htaccess`

## 4. Acessar o painel

Com a estrutura atual (subpasta), a URL é:

```
https://scante.com.br/scante-admin/public/login
```

Login inicial: **admin@scante.com** / **admin123** → **troque a senha logo após entrar.**

## 5. (Opcional, recomendado) URL limpa: scante.com.br

Para o painel abrir em `https://scante.com.br` (sem `/scante-admin/public`), escolha UMA opção:

- **Mudar Document Root** (hPanel → Avançado/Website → apontar para `.../scante-admin/public`), ou
- Mover o conteúdo de `public/` para dentro de `public_html/` e `app/` + `config/` para a pasta
  acima de `public_html/`.

Depois, em `config/config.php`, troque para:
```php
define('APP_URL', 'https://scante.com.br');
```

## Checklist
- [ ] SQL importado (`database_hostinger.sql`)
- [ ] `DB_PASS` preenchida e `config.php` re-enviado
- [ ] `.htaccess` (raiz e public) no servidor
- [ ] SSL ativo (cadeado) no domínio
- [ ] Login OK e senha do admin trocada

## 6. Ambiente de SANDBOX (sandbox.scante.com.br)

O objetivo do sandbox é testar o sistema completo (app Android + painel admin) sem
tocar nos dados/licenças reais de produção.

1. Na Hostinger, crie o subdomínio `sandbox.scante.com.br` apontando pra uma pasta
   separada (ex: `public_html/sandbox/scante-admin/`) — **não** reaproveite a pasta
   de produção.
2. Crie um banco **separado** (hPanel → Bancos de dados), ex: `u508103998_scante_sandbox`,
   e importe `database_hostinger.sql` nele (mesmo processo do passo 1, banco diferente).
3. Copie o código do `scante-admin` pra essa pasta (git clone/pull ou FTP).
4. Copie `config/config.sandbox.example.php` para `config/config.php` **dentro dessa
   pasta de sandbox** e preencha `DB_NAME`/`DB_USER`/`DB_PASS` com o banco do passo 2.
   O `API_SECRET` já vem preenchido igual ao do flavor `sandbox` do app Android — não
   precisa trocar, a menos que você regenere os dois juntos.
5. Envie `.htaccess` (raiz e `public/`) também nessa pasta.
6. No app Android, gere o APK com o flavor `sandbox` (`.\run.ps1` já usa sandbox por
   padrão, ou `.\gradlew.bat assembleSandboxDebug`) — ele aponta pra
   `https://sandbox.scante.com.br/scante-admin/public` automaticamente
   (`app/build.gradle.kts`, flavor `sandbox`). O APK de sandbox instala como app
   separado (`com.logisticapp.emuladortelnet.sandbox`, nome "ScanTE Sandbox"), então
   dá pra ter produção e sandbox no mesmo aparelho ao mesmo tempo.
7. Quando o teste em sandbox validar a mudança, gere a versão de produção com
   `.\run.ps1 -Flavor production` (ou `assembleProductionDebug`/`assembleRelease`).
