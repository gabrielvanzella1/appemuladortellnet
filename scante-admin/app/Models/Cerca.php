<?php
namespace App\Models;

use App\Core\Model;
use DateTimeImmutable;
use DateTimeZone;

/**
 * Cerca digital por Wi-Fi: os coletores informam o roteador (AP/BSSID) em que estão
 * conectados; a empresa marca quais roteadores formam a cerca e dá nome às zonas.
 *
 * Horários no banco em UTC (como o resto do sistema); convertidos para o fuso da
 * operação (America/Sao_Paulo) só para exibir e para avaliar o expediente.
 */
class Cerca extends Model {
    protected string $table = 'dispositivo_sinais';

    public const TZ = 'America/Sao_Paulo';
    public const MAX_SINAIS_POR_ENVIO = 200;
    public const RETENCAO_DIAS = 90;

    public function __construct() {
        parent::__construct();
        // Garante NOW()/UTC_TIMESTAMP coerentes com os horários gravados (UTC),
        // inclusive no MySQL local, que pode estar no fuso da máquina.
        $this->db->execute("SET time_zone = '+00:00'");
    }

    // ------------------------------------------------------------------
    // Regras puras (sem banco) — cobertas por tools/testar_cerca.php
    // ------------------------------------------------------------------

    /** O instante (epoch) cai dentro do expediente da empresa, no fuso da operação? */
    public static function emExpediente(array $empresa, int $ts): bool {
        $local = (new DateTimeImmutable('@' . $ts))->setTimezone(new DateTimeZone(self::TZ));
        $dias  = array_map('intval', explode(',', $empresa['expediente_dias'] ?? '1,2,3,4,5,6'));
        $hora  = $local->format('H:i:s');
        $ini   = $empresa['expediente_inicio'] ?? '06:00:00';
        $fim   = $empresa['expediente_fim'] ?? '22:00:00';

        if ($ini <= $fim) {
            // Turno no mesmo dia (ex.: 06:00–22:00)
            return in_array((int)$local->format('N'), $dias, true) && $hora >= $ini && $hora < $fim;
        }
        // Turno que vira a noite (ex.: 22:00–06:00): depois da meia-noite conta o dia em que começou
        if ($hora >= $ini) return in_array((int)$local->format('N'), $dias, true);
        if ($hora <  $fim) return in_array((int)$local->modify('-1 day')->format('N'), $dias, true);
        return false;
    }

    /**
     * Estado atual do coletor.
     * @param array $cerca conjunto de BSSIDs da cerca (bssid => true)
     */
    public static function estadoDe(array $disp, array $empresa, array $cerca, int $agora): string {
        if (empty($disp['ultimo_sinal_em'])) return 'sem_dados';

        $limite = (int)($empresa['limite_sem_comunicacao_min'] ?? 15) * 60;
        if ($agora - self::utcTs($disp['ultimo_sinal_em']) > $limite) {
            return self::emExpediente($empresa, $agora) ? 'sem_comunicacao' : 'fora_do_expediente';
        }
        if (!$cerca) return 'sem_cerca';
        return isset($cerca[$disp['ultimo_bssid'] ?? '']) ? 'dentro' : 'fora_da_cerca';
    }

    /** Normaliza e valida um sinal vindo do app. Retorna null se inválido. */
    public static function normalizarSinal(array $s, int $agora): ?array {
        $ts = filter_var($s['capturado_em'] ?? null, FILTER_VALIDATE_INT);
        // Aceita até 7 dias de atraso (coletor sem rede) e 5 min de relógio adiantado
        if ($ts === false || $ts < $agora - 7 * 86400 || $ts > $agora + 300) return null;

        $bssid = strtolower(trim((string)($s['bssid'] ?? '')));
        if ($bssid !== '' && !preg_match('/^([0-9a-f]{2}:){5}[0-9a-f]{2}$/', $bssid)) return null;
        // Android devolve 02:00:00:00:00:00 quando não tem permissão para ler o BSSID
        if ($bssid === '02:00:00:00:00:00') $bssid = '';

        $num = fn($v, $min, $max) => (is_numeric($v) && $v >= $min && $v <= $max) ? $v + 0 : null;
        $ssid = trim((string)($s['ssid'] ?? ''), " \"");

        return [
            'ts'         => $ts,
            'em'         => gmdate('Y-m-d H:i:s', $ts),
            'bssid'      => $bssid ?: null,
            'ssid'       => ($ssid === '' || $ssid === '<unknown ssid>') ? null : mb_substr($ssid, 0, 64),
            'rssi'       => $num($s['rssi'] ?? null, -127, 0),
            'bateria'    => $num($s['bateria'] ?? null, 0, 100),
            'carregando' => isset($s['carregando']) ? (int)(bool)$s['carregando'] : null,
            'lat'        => $num($s['lat'] ?? null, -90, 90),
            'lng'        => $num($s['lng'] ?? null, -180, 180),
            'precisao'   => $num($s['precisao'] ?? null, 0, 65535),
        ];
    }

    /** Epoch de um DATETIME gravado em UTC. */
    public static function utcTs(string $utc): int {
        return (new DateTimeImmutable($utc, new DateTimeZone('UTC')))->getTimestamp();
    }

    /** Formata um DATETIME UTC no fuso da operação. */
    public static function local(?string $utc, string $fmt = 'd/m/Y H:i'): string {
        if (!$utc) return '—';
        return (new DateTimeImmutable($utc, new DateTimeZone('UTC')))
            ->setTimezone(new DateTimeZone(self::TZ))->format($fmt);
    }

    /** "há 5 min", "há 2 h"... */
    public static function ha(?string $utc, int $agora): string {
        if (!$utc) return 'nunca';
        $s = max(0, $agora - self::utcTs($utc));
        if ($s < 60)    return 'agora';
        if ($s < 3600)  return 'há ' . intdiv($s, 60) . ' min';
        if ($s < 86400) return 'há ' . intdiv($s, 3600) . ' h';
        return 'há ' . intdiv($s, 86400) . ' d';
    }

    // ------------------------------------------------------------------
    // Recebimento (API)
    // ------------------------------------------------------------------

    /** Dispositivo + parâmetros da empresa dele (null se não tem empresa). */
    public function contextoDoDispositivo(string $deviceId): ?array {
        return $this->db->queryOne(
            "SELECT d.device_id, d.device_nome, d.empresa_id, d.ultimo_sinal_em, d.ultimo_bssid,
                    e.rastreamento_ativo, e.limite_sem_comunicacao_min, e.limite_bateria_pct,
                    e.expediente_inicio, e.expediente_fim, e.expediente_dias
             FROM dispositivos d JOIN empresas e ON e.id = d.empresa_id
             WHERE d.device_id = ?",
            [$deviceId]
        );
    }

    /** Conjunto de BSSIDs que formam a cerca da empresa (bssid => true). */
    public function cercaDaEmpresa(int $empresaId): array {
        $rows = $this->db->query("SELECT bssid FROM pontos_acesso WHERE empresa_id = ? AND na_cerca = 1", [$empresaId]);
        return array_fill_keys(array_column($rows, 'bssid'), true);
    }

    /**
     * Grava os sinais (em ordem de captura), descobre roteadores, abre/fecha alertas
     * e atualiza o estado atual do dispositivo. Retorna quantos sinais foram gravados.
     */
    public function registrarSinais(array $ctx, array $sinaisBrutos, ?string $ip): int {
        $agora   = time();
        $empresa = $ctx;                       // o contexto já traz os parâmetros da empresa
        $eid     = (int)$ctx['empresa_id'];
        $device  = $ctx['device_id'];
        $nome    = $ctx['device_nome'];

        $sinais = [];
        foreach (array_slice($sinaisBrutos, 0, self::MAX_SINAIS_POR_ENVIO) as $s) {
            if (is_array($s) && ($n = self::normalizarSinal($s, $agora))) $sinais[] = $n;
        }
        if (!$sinais) return 0;
        usort($sinais, fn($a, $b) => $a['ts'] <=> $b['ts']);

        $cerca        = $this->cercaDaEmpresa($eid);
        $limiteSeg    = (int)$empresa['limite_sem_comunicacao_min'] * 60;
        $limiteBat    = (int)$empresa['limite_bateria_pct'];
        $prevEm       = $ctx['ultimo_sinal_em'];
        $prevBssid    = $ctx['ultimo_bssid'];

        foreach ($sinais as $s) {
            // Sinal atrasado (anterior ao último já processado) só entra no histórico
            $emOrdem = !$prevEm || $s['ts'] >= self::utcTs($prevEm);

            $this->db->execute(
                "INSERT INTO dispositivo_sinais
                    (device_id, empresa_id, bssid, ssid, rssi, lat, lng, precisao_m, bateria_pct, carregando, ip, capturado_em)
                 VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                [$device, $eid, $s['bssid'], $s['ssid'], $s['rssi'], $s['lat'], $s['lng'], $s['precisao'],
                 $s['bateria'], $s['carregando'], $ip, $s['em']]
            );
            if ($s['bssid']) {
                $this->db->execute(
                    "INSERT INTO pontos_acesso (empresa_id, bssid, ssid, primeiro_visto, ultimo_visto)
                     VALUES (?,?,?,?,?)
                     ON DUPLICATE KEY UPDATE ssid = COALESCE(VALUES(ssid), ssid),
                                             ultimo_visto = GREATEST(ultimo_visto, VALUES(ultimo_visto))",
                    [$eid, $s['bssid'], $s['ssid'], $s['em'], $s['em']]
                );
            }
            if (!$emOrdem) continue;

            // Voltou depois de um sumiço maior que o limite: registra o período sem comunicação
            if ($prevEm && $s['ts'] - self::utcTs($prevEm) > $limiteSeg
                && self::emExpediente($empresa, self::utcTs($prevEm))) {
                $min = intdiv($s['ts'] - self::utcTs($prevEm), 60);
                $this->abrirAlerta($eid, $device, $nome, 'sem_comunicacao', $prevEm, $prevBssid,
                    "Sem sinal por {$min} min", $s['em']);
            }

            // Cerca (só depois que a empresa marcou algum roteador)
            if ($cerca) {
                $dentro = $s['bssid'] && isset($cerca[$s['bssid']]);
                $aberto = $this->alertaAberto($device, 'fora_da_cerca');
                if (!$dentro && !$aberto) {
                    $det = $s['bssid']
                        ? 'Conectado a ' . ($s['ssid'] ?? 'rede') . " ({$s['bssid']}), fora da cerca"
                        : 'Sem Wi-Fi';
                    $this->abrirAlerta($eid, $device, $nome, 'fora_da_cerca', $s['em'], $s['bssid'], $det);
                } elseif ($dentro && $aberto) {
                    $this->fecharAlerta((int)$aberto['id'], $s['em']);
                }
            }

            // Bateria baixa (fecha ao carregar ou ao passar de limite + 5%). Só abre quando o app
            // afirma que NÃO está carregando — sem essa informação não dá pra saber, e não alarma.
            if ($s['bateria'] !== null) {
                $aberto = $this->alertaAberto($device, 'bateria_baixa');
                if (!$aberto && $s['bateria'] < $limiteBat && $s['carregando'] === 0) {
                    $this->abrirAlerta($eid, $device, $nome, 'bateria_baixa', $s['em'], $s['bssid'], "Bateria em {$s['bateria']}%");
                } elseif ($aberto && ($s['carregando'] || $s['bateria'] >= $limiteBat + 5)) {
                    $this->fecharAlerta((int)$aberto['id'], $s['em']);
                }
            }

            $prevEm = $s['em'];
            if ($s['bssid']) $prevBssid = $s['bssid'];
            $ultimo = $s;
        }

        if (isset($ultimo)) {
            $this->db->execute(
                "UPDATE dispositivos SET ultimo_sinal_em = ?, ultimo_bssid = ?, ultimo_ssid = ?, ultimo_rssi = ?,
                        ultima_bateria_pct = COALESCE(?, ultima_bateria_pct), ultimo_carregando = COALESCE(?, ultimo_carregando),
                        ultima_lat = COALESCE(?, ultima_lat), ultima_lng = COALESCE(?, ultima_lng), ultimo_acesso = UTC_TIMESTAMP()
                 WHERE device_id = ?",
                [$ultimo['em'], $ultimo['bssid'], $ultimo['ssid'], $ultimo['rssi'], $ultimo['bateria'],
                 $ultimo['carregando'], $ultimo['lat'], $ultimo['lng'], $device]
            );
        }

        // Retenção sem cron: apaga um lote pequeno de sinais vencidos a cada envio
        $this->db->execute(
            "DELETE FROM dispositivo_sinais WHERE capturado_em < UTC_TIMESTAMP() - INTERVAL " . self::RETENCAO_DIAS . " DAY LIMIT 500"
        );
        return count($sinais);
    }

    private function alertaAberto(string $device, string $tipo): ?array {
        return $this->db->queryOne(
            "SELECT id FROM dispositivo_alertas WHERE device_id = ? AND tipo = ? AND fim_em IS NULL ORDER BY id DESC LIMIT 1",
            [$device, $tipo]
        );
    }

    private function abrirAlerta(int $eid, string $device, ?string $nome, string $tipo, string $inicio,
                                 ?string $bssid, string $detalhe, ?string $fim = null): void {
        $this->db->execute(
            "INSERT INTO dispositivo_alertas (empresa_id, device_id, device_nome, tipo, inicio_em, fim_em, bssid, detalhe)
             VALUES (?,?,?,?,?,?,?,?)",
            [$eid, $device, $nome, $tipo, $inicio, $fim, $bssid, mb_substr($detalhe, 0, 200)]
        );
    }

    private function fecharAlerta(int $id, string $fim): void {
        $this->db->execute("UPDATE dispositivo_alertas SET fim_em = ? WHERE id = ?", [$fim, $id]);
    }

    // ------------------------------------------------------------------
    // Leitura (painel da empresa)
    // ------------------------------------------------------------------

    public function empresa(int $empresaId): ?array {
        return $this->db->queryOne("SELECT * FROM empresas WHERE id = ?", [$empresaId]);
    }

    /** Coletores da empresa com zona e estado calculado. */
    public function dispositivos(array $empresa): array {
        $cerca = $this->cercaDaEmpresa((int)$empresa['id']);
        $agora = time();
        $rows = $this->db->query(
            "SELECT d.*, pa.zona, pa.na_cerca
             FROM dispositivos d
             LEFT JOIN pontos_acesso pa ON pa.empresa_id = d.empresa_id AND pa.bssid = d.ultimo_bssid
             WHERE d.empresa_id = ?
             ORDER BY d.device_nome",
            [(int)$empresa['id']]
        );
        foreach ($rows as &$r) $r['estado'] = self::estadoDe($r, $empresa, $cerca, $agora);
        return $rows;
    }

    /** Alertas abertos: os gravados + "sem comunicação" calculado agora (sem cron). */
    public function alertasAbertos(array $empresa, array $dispositivos): array {
        $abertos = $this->db->query(
            "SELECT a.*, pa.zona FROM dispositivo_alertas a
             LEFT JOIN pontos_acesso pa ON pa.empresa_id = a.empresa_id AND pa.bssid = a.bssid
             WHERE a.empresa_id = ? AND a.fim_em IS NULL ORDER BY a.inicio_em DESC",
            [(int)$empresa['id']]
        );
        foreach ($dispositivos as $d) {
            if ($d['estado'] !== 'sem_comunicacao') continue;
            $abertos[] = [
                'tipo' => 'sem_comunicacao', 'device_id' => $d['device_id'], 'device_nome' => $d['device_nome'],
                'inicio_em' => $d['ultimo_sinal_em'], 'fim_em' => null, 'bssid' => $d['ultimo_bssid'],
                'zona' => $d['zona'], 'detalhe' => 'Último sinal ' . self::ha($d['ultimo_sinal_em'], time()),
            ];
        }
        usort($abertos, fn($a, $b) => strcmp($b['inicio_em'], $a['inicio_em']));
        return $abertos;
    }

    /** Roteadores da empresa, com quantos coletores estão neles agora. */
    public function pontos(array $empresa): array {
        return $this->db->query(
            "SELECT pa.*,
                    (SELECT COUNT(*) FROM dispositivos d
                     WHERE d.empresa_id = pa.empresa_id AND d.ultimo_bssid = pa.bssid
                       AND d.ultimo_sinal_em >= UTC_TIMESTAMP() - INTERVAL ? MINUTE) AS coletores_agora
             FROM pontos_acesso pa WHERE pa.empresa_id = ?
             ORDER BY pa.na_cerca DESC, pa.zona IS NULL, pa.zona, pa.ultimo_visto DESC",
            [(int)$empresa['limite_sem_comunicacao_min'], (int)$empresa['id']]
        );
    }

    /** Marca quais roteadores são da cerca e o nome da zona de cada um. */
    public function salvarPontos(int $empresaId, array $naCerca, array $zonas): void {
        foreach ($this->db->query("SELECT id FROM pontos_acesso WHERE empresa_id = ?", [$empresaId]) as $p) {
            $id   = (int)$p['id'];
            $zona = trim((string)($zonas[$id] ?? ''));
            $this->db->execute(
                "UPDATE pontos_acesso SET na_cerca = ?, zona = ? WHERE id = ? AND empresa_id = ?",
                [isset($naCerca[$id]) ? 1 : 0, $zona === '' ? null : mb_substr($zona, 0, 100), $id, $empresaId]
            );
        }
    }

    /** Histórico de alertas gravados nos últimos N dias. */
    public function alertas(int $empresaId, ?string $tipo, int $dias): array {
        $sql = "SELECT a.*, pa.zona FROM dispositivo_alertas a
                LEFT JOIN pontos_acesso pa ON pa.empresa_id = a.empresa_id AND pa.bssid = a.bssid
                WHERE a.empresa_id = ? AND a.inicio_em >= UTC_TIMESTAMP() - INTERVAL ? DAY";
        $params = [$empresaId, $dias];
        if ($tipo) { $sql .= " AND a.tipo = ?"; $params[] = $tipo; }
        return $this->db->query($sql . " ORDER BY a.inicio_em DESC LIMIT 500", $params);
    }

    public function dispositivoDaEmpresa(int $empresaId, string $deviceId): ?array {
        return $this->db->queryOne("SELECT * FROM dispositivos WHERE empresa_id = ? AND device_id = ?", [$empresaId, $deviceId]);
    }

    /**
     * Linha do tempo de um dia (no fuso da operação): sinais consecutivos no mesmo
     * roteador viram um trecho; intervalos maiores que o limite viram "sem sinal".
     */
    public function linhaDoTempo(array $empresa, string $deviceId, string $diaLocal): array {
        $tz  = new DateTimeZone(self::TZ);
        $ini = (new DateTimeImmutable($diaLocal . ' 00:00:00', $tz))->setTimezone(new DateTimeZone('UTC'));
        $fim = $ini->modify('+1 day');
        $rows = $this->db->query(
            "SELECT s.capturado_em, s.bssid, s.ssid, s.bateria_pct, pa.zona, pa.na_cerca
             FROM dispositivo_sinais s
             LEFT JOIN pontos_acesso pa ON pa.empresa_id = s.empresa_id AND pa.bssid = s.bssid
             WHERE s.empresa_id = ? AND s.device_id = ? AND s.capturado_em >= ? AND s.capturado_em < ?
             ORDER BY s.capturado_em",
            [(int)$empresa['id'], $deviceId, $ini->format('Y-m-d H:i:s'), $fim->format('Y-m-d H:i:s')]
        );
        return self::agruparTrechos($rows, (int)$empresa['limite_sem_comunicacao_min'] * 60);
    }

    /** Agrupa sinais em trechos (mesmo roteador) e buracos (sem sinal). Puro — testado. */
    public static function agruparTrechos(array $sinais, int $limiteSeg): array {
        $trechos = [];
        $atual = null;
        foreach ($sinais as $s) {
            $ts = self::utcTs($s['capturado_em']);
            if ($atual && $ts - $atual['fim_ts'] > $limiteSeg) {
                $trechos[] = $atual;
                $trechos[] = ['tipo' => 'sem_sinal', 'inicio_ts' => $atual['fim_ts'], 'fim_ts' => $ts];
                $atual = null;
            }
            if ($atual && $atual['bssid'] === $s['bssid']) {
                $atual['fim_ts'] = $ts;
                $atual['sinais']++;
                if ($s['bateria_pct'] !== null) $atual['bateria'] = (int)$s['bateria_pct'];
                continue;
            }
            if ($atual) { $atual['fim_ts'] = $ts; $trechos[] = $atual; }
            $atual = [
                'tipo' => 'ap', 'bssid' => $s['bssid'], 'ssid' => $s['ssid'], 'zona' => $s['zona'],
                'na_cerca' => (int)($s['na_cerca'] ?? 0), 'inicio_ts' => $ts, 'fim_ts' => $ts, 'sinais' => 1,
                'bateria' => $s['bateria_pct'] !== null ? (int)$s['bateria_pct'] : null,
            ];
        }
        if ($atual) $trechos[] = $atual;
        return $trechos;
    }

    public function salvarConfig(int $empresaId, array $cfg): void {
        $this->db->execute(
            "UPDATE empresas SET limite_sem_comunicacao_min = ?, limite_bateria_pct = ?,
                    expediente_inicio = ?, expediente_fim = ?, expediente_dias = ? WHERE id = ?",
            [$cfg['limite_sem_comunicacao_min'], $cfg['limite_bateria_pct'], $cfg['expediente_inicio'],
             $cfg['expediente_fim'], $cfg['expediente_dias'], $empresaId]
        );
    }
}
