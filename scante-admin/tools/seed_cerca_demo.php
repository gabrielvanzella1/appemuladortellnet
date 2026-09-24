<?php
// Prepara uma empresa para demonstrar a Cerca Wi-Fi: 8 coletores simulados (SIM-*) e os
// 5 roteadores do "CD" já nomeados. Idempotente. Nunca publicar esta pasta no servidor.
//   php tools/seed_cerca_demo.php --empresa=2            (local)
//   php tools/seed_cerca_demo.php --empresa=2 --limpar   (remove tudo que é SIM-*)
// No servidor: ssh ... 'cd <admin> && php -- --empresa=4' < tools/seed_cerca_demo.php
if (PHP_SAPI !== 'cli') exit;
require is_file(__DIR__ . '/../config/config.php') ? __DIR__ . '/../config/config.php' : 'config/config.php';

$opt = getopt('', ['empresa:', 'limpar']);
$eid = (int)($opt['empresa'] ?? 0);
if (!$eid) exit("Informe --empresa=ID\n");

$pdo = new PDO('mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4', DB_USER, DB_PASS,
    [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]);
$emp = $pdo->query("SELECT id, nome FROM empresas WHERE id = $eid")->fetch(PDO::FETCH_ASSOC);
if (!$emp) exit("Empresa $eid nao existe\n");

// Remove dados simulados anteriores (sinais, alertas, dispositivos SIM-* e roteadores de demo)
$pdo->exec("DELETE FROM dispositivo_sinais  WHERE device_id LIKE 'SIM-%'");
$pdo->exec("DELETE FROM dispositivo_alertas WHERE device_id LIKE 'SIM-%'");
$pdo->exec("DELETE FROM dispositivos        WHERE device_id LIKE 'SIM-%'");
$pdo->exec("DELETE FROM pontos_acesso WHERE bssid LIKE 'c0:ff:ee:%'");
if (isset($opt['limpar'])) exit("Dados simulados removidos ({$emp['nome']}).\n");

$coletores = ['Coletor 01', 'Coletor 02', 'Coletor 03', 'Coletor 04', 'Coletor 05', 'Coletor 06', 'Coletor 07', 'Coletor 08'];
$ins = $pdo->prepare("INSERT INTO dispositivos (device_id, device_nome, app_version, empresa_id, empresa_nome, status_licenca, primeiro_acesso, ultimo_acesso)
                      VALUES (?, ?, '1.2.0', ?, ?, 'ativa', UTC_TIMESTAMP(), UTC_TIMESTAMP())");
foreach ($coletores as $i => $nome) {
    $ins->execute([sprintf('SIM-%02d', $i + 1), "$nome (simulado)", $eid, $emp['nome']]);
}

$zonas = ['Recebimento', 'Estoque', 'Separação', 'Expedição', 'Doca'];
$ap = $pdo->prepare("INSERT INTO pontos_acesso (empresa_id, bssid, ssid, zona, na_cerca) VALUES (?, ?, 'CD-WIFI', ?, 1)");
foreach ($zonas as $i => $z) $ap->execute([$eid, sprintf('c0:ff:ee:00:00:%02d', $i + 1), $z]);

$pdo->exec("UPDATE empresas SET rastreamento_ativo = 1 WHERE id = $eid");
echo "Pronto: {$emp['nome']} com cerca ativa, " . count($coletores) . " coletores SIM-* e " . count($zonas) . " zonas.\n";
