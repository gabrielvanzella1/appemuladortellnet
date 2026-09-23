<?php
// Teste da Cerca Wi-Fi: regras puras + cenário completo no banco LOCAL (scante_dev).
// Uso: php tools/testar_cerca.php      (nunca publicar esta pasta no servidor)
declare(strict_types=1);
if (PHP_SAPI !== 'cli') exit;

spl_autoload_register(function (string $class) {
    $file = __DIR__ . '/../' . str_replace(['App\\', '\\'], ['app/', '/'], $class) . '.php';
    if (file_exists($file)) require_once $file;
});
require_once __DIR__ . '/../config/config.php';
if (DB_NAME !== 'scante_dev') exit("Recusado: este teste so roda no banco local scante_dev (atual: " . DB_NAME . ")\n");

use App\Models\Cerca;
use App\Core\Database;

$ok = 0;
function check(bool $cond, string $msg): void { global $ok; if (!$cond) { echo "FALHOU: $msg\n"; exit(1); } $ok++; }
$tsLocal = fn(string $s) => (new DateTimeImmutable($s, new DateTimeZone(Cerca::TZ)))->getTimestamp();

// ---------------- Regras puras ----------------
$turno = ['expediente_inicio' => '06:00:00', 'expediente_fim' => '22:00:00', 'expediente_dias' => '1,2,3,4,5,6'];
check(Cerca::emExpediente($turno, $tsLocal('2026-09-21 10:00')),  'segunda 10h esta no expediente');
check(!Cerca::emExpediente($turno, $tsLocal('2026-09-20 10:00')), 'domingo fora');
check(!Cerca::emExpediente($turno, $tsLocal('2026-09-21 23:00')), 'segunda 23h fora');
check(!Cerca::emExpediente($turno, $tsLocal('2026-09-21 05:59')), 'segunda 05:59 fora');

$noite = ['expediente_inicio' => '22:00:00', 'expediente_fim' => '06:00:00', 'expediente_dias' => '1,2,3,4,5'];
check(Cerca::emExpediente($noite, $tsLocal('2026-09-25 23:00')),  'turno da noite: sexta 23h dentro');
check(Cerca::emExpediente($noite, $tsLocal('2026-09-26 02:00')),  'sabado 02h conta como turno de sexta');
check(!Cerca::emExpediente($noite, $tsLocal('2026-09-26 23:00')), 'sabado 23h fora (sabado nao e dia de turno)');
check(!Cerca::emExpediente($noite, $tsLocal('2026-09-21 03:00')), 'segunda 03h seria turno de domingo: fora');

$emp   = $turno + ['limite_sem_comunicacao_min' => 15];
$agora = $tsLocal('2026-09-21 10:00');
$sinal = fn(int $segAtras, ?string $bssid) => ['ultimo_sinal_em' => gmdate('Y-m-d H:i:s', $agora - $segAtras), 'ultimo_bssid' => $bssid];
$cerca = ['aa:aa:aa:aa:aa:01' => true];
check(Cerca::estadoDe(['ultimo_sinal_em' => null], $emp, $cerca, $agora) === 'sem_dados', 'sem dados');
check(Cerca::estadoDe($sinal(60, 'aa:aa:aa:aa:aa:01'), $emp, $cerca, $agora) === 'dentro', 'dentro');
check(Cerca::estadoDe($sinal(60, 'bb:bb:bb:bb:bb:bb'), $emp, $cerca, $agora) === 'fora_da_cerca', 'fora da cerca');
check(Cerca::estadoDe($sinal(60, 'bb:bb:bb:bb:bb:bb'), $emp, [], $agora) === 'sem_cerca', 'sem cerca configurada');
check(Cerca::estadoDe($sinal(1200, 'aa:aa:aa:aa:aa:01'), $emp, $cerca, $agora) === 'sem_comunicacao', '20 min sem sinal no expediente');
check(Cerca::estadoDe($sinal(1200, 'aa:aa:aa:aa:aa:01'), $emp, $cerca, $tsLocal('2026-09-21 23:00')) === 'fora_do_expediente', 'sem sinal fora do expediente');

$n = time();
check(Cerca::normalizarSinal(['capturado_em' => $n, 'bssid' => 'zz'], $n) === null, 'bssid invalido recusa o sinal');
check(Cerca::normalizarSinal(['capturado_em' => $n - 8 * 86400], $n) === null, 'sinal com mais de 7 dias recusado');
$s = Cerca::normalizarSinal(['capturado_em' => $n, 'bssid' => '02:00:00:00:00:00', 'ssid' => '"CENTAURO"', 'rssi' => 5, 'bateria' => 50], $n);
check($s !== null && $s['bssid'] === null, 'bssid mascarado do Android vira null');
check($s['ssid'] === 'CENTAURO' && $s['rssi'] === null && $s['bateria'] === 50, 'ssid sem aspas, rssi fora da faixa descartado');

$t0 = 1758450000;
$row = fn(int $ts, string $b) => ['capturado_em' => gmdate('Y-m-d H:i:s', $ts), 'bssid' => $b, 'ssid' => 'X', 'zona' => null, 'na_cerca' => 1, 'bateria_pct' => null];
$tr = Cerca::agruparTrechos([$row($t0, 'A'), $row($t0 + 60, 'A'), $row($t0 + 120, 'B'), $row($t0 + 2000, 'B')], 900);
check(count($tr) === 4, 'trechos: A, B, sem sinal, B');
check($tr[0]['bssid'] === 'A' && $tr[0]['fim_ts'] === $t0 + 120 && $tr[0]['sinais'] === 2, 'trecho A vai ate a troca para B');
check($tr[2]['tipo'] === 'sem_sinal' && $tr[2]['fim_ts'] - $tr[2]['inicio_ts'] === 1880, 'buraco de 1880s');

// ---------------- Cenario completo no banco local ----------------
$db  = Database::getInstance();
$eid = 2; // "Centauro (demo)" no scante_dev
$dev = 'SIM-TESTE-1';
$aps = ['a1:a1:a1:a1:a1:01', 'a1:a1:a1:a1:a1:02', 'ff:ff:ff:ff:ff:03'];
$limpar = function () use ($db, $dev, $eid, $aps) {
    $db->execute("DELETE FROM dispositivo_sinais  WHERE device_id = ?", [$dev]);
    $db->execute("DELETE FROM dispositivo_alertas WHERE device_id = ?", [$dev]);
    $db->execute("DELETE FROM dispositivos        WHERE device_id = ?", [$dev]);
    $db->execute("DELETE FROM pontos_acesso WHERE empresa_id = ? AND bssid IN (?,?,?)", array_merge([$eid], $aps));
};
$limpar();
$orig = $db->queryOne("SELECT rastreamento_ativo, limite_sem_comunicacao_min, limite_bateria_pct, expediente_inicio, expediente_fim, expediente_dias FROM empresas WHERE id = ?", [$eid]);
$db->execute("UPDATE empresas SET rastreamento_ativo = 1, limite_sem_comunicacao_min = 15, limite_bateria_pct = 15,
              expediente_inicio = '00:00:00', expediente_fim = '23:59:59', expediente_dias = '1,2,3,4,5,6,7' WHERE id = ?", [$eid]);
$db->execute("INSERT INTO dispositivos (device_id, device_nome, empresa_id, status_licenca) VALUES (?, 'Coletor Simulado 1', ?, 'ativa')", [$dev, $eid]);
$db->execute("INSERT INTO pontos_acesso (empresa_id, bssid, ssid, zona, na_cerca) VALUES (?,?,?,?,1), (?,?,?,?,1)",
    [$eid, $aps[0], 'CD', 'Doca', $eid, $aps[1], 'CD', 'Estoque']);

try {
    $cerca = new Cerca();
    $now = time();
    $sinais = [
        ['capturado_em' => $now - 3600, 'bssid' => $aps[0], 'ssid' => 'CD', 'bateria' => 80],                         // dentro (Doca)
        ['capturado_em' => $now - 3000, 'bssid' => $aps[2], 'ssid' => 'VIZINHO', 'bateria' => 78],                    // saiu da cerca
        ['capturado_em' => $now - 2940, 'bssid' => $aps[1], 'ssid' => 'CD', 'bateria' => 78],                         // voltou (Estoque)
        ['capturado_em' => $now - 600,  'bssid' => $aps[1], 'ssid' => 'CD', 'bateria' => 10, 'carregando' => false],  // sumiu 39 min + bateria baixa
        ['capturado_em' => $now - 60,   'bssid' => $aps[1], 'ssid' => 'CD', 'bateria' => 12, 'carregando' => true],   // carregando
    ];
    shuffle($sinais); // a ordem de chegada nao pode importar
    $ctx = $cerca->contextoDoDispositivo($dev);
    check($ctx !== null && (int)$ctx['rastreamento_ativo'] === 1, 'contexto do dispositivo com rastreamento');
    check($cerca->registrarSinais($ctx, $sinais, '127.0.0.1') === 5, '5 sinais gravados');

    $al = [];
    foreach ($db->query("SELECT * FROM dispositivo_alertas WHERE device_id = ? ORDER BY inicio_em", [$dev]) as $a) $al[$a['tipo']] = $a;
    check(count($al) === 3, 'tres alertas: fora da cerca, sem comunicacao, bateria (' . implode(',', array_keys($al)) . ')');
    check($al['fora_da_cerca']['bssid'] === $aps[2] && $al['fora_da_cerca']['fim_em'] === gmdate('Y-m-d H:i:s', $now - 2940), 'fora da cerca fechou ao voltar');
    check($al['sem_comunicacao']['inicio_em'] === gmdate('Y-m-d H:i:s', $now - 2940) && $al['sem_comunicacao']['fim_em'] === gmdate('Y-m-d H:i:s', $now - 600), 'periodo sem comunicacao gravado');
    check($al['sem_comunicacao']['bssid'] === $aps[1], 'sem comunicacao guarda o ultimo roteador visto');
    check($al['bateria_baixa']['fim_em'] === gmdate('Y-m-d H:i:s', $now - 60), 'bateria baixa fechou ao carregar');

    $d = $db->queryOne("SELECT * FROM dispositivos WHERE device_id = ?", [$dev]);
    check($d['ultimo_bssid'] === $aps[1] && (int)$d['ultima_bateria_pct'] === 12 && (int)$d['ultimo_carregando'] === 1, 'estado atual do dispositivo');
    $emp2 = $cerca->empresa($eid);
    $lista = $cerca->dispositivos($emp2);
    $meu = array_values(array_filter($lista, fn($x) => $x['device_id'] === $dev))[0];
    check($meu['estado'] === 'dentro' && $meu['zona'] === 'Estoque', 'painel mostra dentro / Estoque');
    $novo = $db->queryOne("SELECT na_cerca FROM pontos_acesso WHERE empresa_id = ? AND bssid = ?", [$eid, $aps[2]]);
    check($novo !== null && (int)$novo['na_cerca'] === 0, 'roteador vizinho descoberto sozinho, fora da cerca');

    // Reenvio (app mandou de novo) nao duplica alertas
    $cerca->registrarSinais($cerca->contextoDoDispositivo($dev), [['capturado_em' => $now - 30, 'bssid' => $aps[1], 'bateria' => 13]], null);
    check((int)$db->queryOne("SELECT COUNT(*) n FROM dispositivo_alertas WHERE device_id = ?", [$dev])['n'] === 3, 'nenhum alerta novo com sinal normal');
} finally {
    $limpar();
    $db->execute("UPDATE empresas SET rastreamento_ativo=?, limite_sem_comunicacao_min=?, limite_bateria_pct=?, expediente_inicio=?, expediente_fim=?, expediente_dias=? WHERE id=?",
        array_merge(array_values($orig), [$eid]));
}
echo "OK: $ok verificacoes passaram\n";
