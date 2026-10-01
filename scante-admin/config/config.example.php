<?php
// Copie este arquivo para config.php e preencha com seus valores

// Configurações gerais
define('APP_NAME', 'ScanTE Admin');
define('APP_URL',  'http://localhost:8099');  // URL base do painel
define('APP_VERSION', '1.0.0');

// Banco de dados
define('DB_HOST', 'localhost');
define('DB_NAME', 'scante_admin');
define('DB_USER', 'root');
define('DB_PASS', '');
define('DB_CHARSET', 'utf8mb4');

// Sessão
define('SESSION_NAME', 'scante_session');
define('SESSION_LIFETIME', 7200); // 2 horas

// Chave secreta para tokens da API (altere para uma string aleatória longa)
define('API_SECRET', 'MUDE_PARA_UMA_CHAVE_SECRETA_FORTE');

// Trial do app (dias)
define('TRIAL_DIAS', 7);

// E-mail remetente usado no cabeçalho "From" dos e-mails do sistema
define('MAIL_FROM', 'noreply@scante.com.br');

// Chave PRIVADA (Ed25519, base64) usada para assinar as licenças do ScanTE Relay.
// Gere a sua com: php -r "$kp=sodium_crypto_sign_keypair(); echo base64_encode(sodium_crypto_sign_secretkey($kp));"
// A chave PÚBLICA correspondente precisa ser embutida em scante-relay/license.go.
define('RELAY_LICENSE_PRIVATE_KEY', 'GERE_A_SUA_CHAVE_PRIVADA_AQUI');
