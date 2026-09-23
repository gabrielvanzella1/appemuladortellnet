<?php
// Simula coletores mandando sinais Wi-Fi pela API (/api/dispositivo/sinal).
// Rode antes o seed_cerca_demo.php na mesma base. Nunca publicar esta pasta no servidor.
//   php tools/simular_cerca.php --url=http://localhost:8099 --secret=XXXX            (histórico de hoje)
//   php tools/simular_cerca.php --url=... --secret=... --ao-vivo=30                  (1 sinal/min por 30 min)
if (PHP_SAPI !== 'cli') exit;

$opt    = getopt('', ['url:', 'secret:', 'ao-vivo::']);
$url    = rtrim($opt['url'] ?? '', '/') . '/api/dispositivo/sinal';
$secret = $opt['secret'] ?? '';
if (!$secret || !isset($opt['url'])) exit("Uso: --url=BASE --secret=API_SECRET [--ao-vivo=MINUTOS]\n");

$zonas   = array_map(fn($i) => sprintf('c0:ff:ee:00:00:%02d', $i), range(1, 5));   // Recebimento..Doca
$vizinho = 'c0:ff:ee:99:99:99';                                                      // Wi-Fi fora do CD
$tz      = new DateTimeZone('America/Sao_Paulo');
$agora   = time();
$inicio  = max($agora - 6 * 3600, (new DateTimeImmutable('today 06:00', $tz))->getTimestamp());

function enviar(string $url, string $secret, string $device, array $sinais): array {
    $ctx = stream_context_create(['http' => [
        'method' => 'POST', 'ignore_errors' => true, 'timeout' => 20,
        'header' => "Content-Type: application/json\r\nAuthorization: Bearer $secret\r\n",
        'content' => json_encode(['device_id' => $device, 'sinais' => $sinais]),
    ]]);
    $resp = @file_get_contents($url, false, $ctx);
    return json_decode((string)$resp, true) ?? ['ok' => false, 'erro' => substr((string)$resp, 0, 200)];
}

// Monta a sequência de um coletor: caminha pelas zonas, a cada 2 min, com comportamentos especiais
function historico(int $n, int $ini, int $fim, array $zonas, string $vizinho): array {
    mt_srand($n * 7919);
    $sinais = [];
    $z = $n % count($zonas);
    $bat = 100 - $n * 3;
    for ($t = $ini; $t <= $fim; $t += 120) {
        if (mt_rand(0, 9) === 0) $z = ($z + (mt_rand(0, 1) ? 1 : count($zonas) - 1)) % count($zonas); // troca de zona
        $bssid = $zonas[$z];
        $bat = max(3, $bat - (mt_rand(0, 3) === 0 ? 1 : 0));
        $carregando = false;

        if ($n === 7 && $t >= $fim - 5400 && $t < $fim - 4500) $bssid = $vizinho;      // saiu da cerca 15 min
        if ($n === 8 && $t > $fim - 2400) break;                                         // sumiu há 40 min
        if ($n === 5) { $bat = max(9, 60 - intdiv($t - $ini, 420)); }                    // bateria acabando
        if ($n === 3 && $t > $fim - 1800) { $carregando = true; $bat = min(100, $bat + 2); } // no carregador

        $sinais[] = ['capturado_em' => $t, 'bssid' => $bssid, 'ssid' => $bssid === $vizinho ? 'VIZINHO-WIFI' : 'CD-WIFI',
                     'rssi' => -45 - mt_rand(0, 30), 'bateria' => $bat, 'carregando' => $carregando];
    }
    return $sinais;
}

if (!isset($opt['ao-vivo'])) {
    for ($n = 1; $n <= 8; $n++) {
        $device = sprintf('SIM-%02d', $n);
        $total = 0;
        foreach (array_chunk(historico($n, $inicio, $agora, $zonas, $vizinho), 200) as $lote) {
            $r = enviar($url, $secret, $device, $lote);
            if (empty($r['ok'])) exit("$device: erro " . json_encode($r) . "\n");
            $total += $r['gravados'] ?? 0;
        }
        echo "$device: $total sinais" . (empty($r['rastreamento']) ? ' (IGNORADO: empresa sem rastreamento/dispositivo sem empresa)' : '') . "\n";
    }
    exit;
}

// Ao vivo: mantém os coletores "vivos" (menos o 08, que segue sumido) mandando 1 sinal por minuto
$minutos = max(1, (int)$opt['ao-vivo']);
for ($m = 0; $m < $minutos; $m++) {
    foreach ([1, 2, 3, 4, 5, 6, 7] as $n) {
        $s = historico($n, time() - 120, time(), $zonas, $vizinho);
        enviar($url, $secret, sprintf('SIM-%02d', $n), [end($s)]);
    }
    echo date('H:i:s') . " sinais enviados\n";
    if ($m < $minutos - 1) sleep(60);
}
