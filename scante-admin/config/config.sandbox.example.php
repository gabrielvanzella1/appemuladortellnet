<?php
// ============================================================
// ScanTE Admin — Configuração de SANDBOX (sandbox.scante.com.br)
// Copie este arquivo para config.php DENTRO do servidor/pasta do
// subdomínio de sandbox na Hostinger (nunca no mesmo config.php da produção).
// ============================================================

define('APP_NAME', 'ScanTE Admin (Sandbox)');
define('APP_VERSION', '1.0.0');

// URL base do painel no subdomínio de teste — SEM barra no final.
define('APP_URL', 'https://sandbox.scante.com.br/scante-admin/public');

// Banco de dados — use um banco SEPARADO do de produção na Hostinger
// (ex: criar "u508103998_scante_sandbox" em hPanel > Bancos de dados).
define('DB_HOST', 'localhost');
define('DB_NAME', 'TROQUE_PARA_O_BANCO_DE_SANDBOX');
define('DB_USER', 'TROQUE_PARA_O_USUARIO_DE_SANDBOX');
define('DB_PASS', 'TROQUE_PARA_A_SENHA_DE_SANDBOX');
define('DB_CHARSET', 'utf8mb4');

// Sessão
define('SESSION_NAME', 'scante_session_sandbox');
define('SESSION_LIFETIME', 7200); // 2 horas

// Chave secreta para tokens da API — DEVE ser idêntica ao BuildConfig.API_SECRET
// do flavor "sandbox" em EmuladorTelnet/app/build.gradle.kts.
define('API_SECRET', 'c9ce24d6a1e9a583036278736526a82d144e9955b04c9ea490f215f230f592cf');

// Pagar.me — sandbox usa sempre chaves sk_test_ / pk_test_
define('PAGARME_SECRET_KEY', 'sk_test_SUA_CHAVE_SECRETA');
define('PAGARME_PUBLIC_KEY',  'pk_test_SUA_CHAVE_PUBLICA');

// Preços das licenças (R$) — pode manter iguais à produção pra teste realista
define('PRECO_MENSAL',    29.90);
define('PRECO_ANUAL',    199.90);
define('PRECO_VITALICIA', 499.90);

// Trial do app (dias)
define('TRIAL_DIAS', 7);

// E-mail remetente usado no cabeçalho "From" dos e-mails do sistema
define('MAIL_FROM', 'noreply@scante.com.br');

// Chave PRIVADA (Ed25519, base64) usada para assinar licenças do ScanTE Relay.
// Só precisa bater com a chave pública embutida em scante-relay/license.go se
// você for testar emissão de licença do Relay a partir do sandbox também.
// Gere a sua com: php -r "$kp=sodium_crypto_sign_keypair(); echo base64_encode(sodium_crypto_sign_secretkey($kp));"
define('RELAY_LICENSE_PRIVATE_KEY', 'GERE_A_SUA_CHAVE_PRIVADA_DE_SANDBOX_AQUI');
