<?php
namespace App\Controllers\Api;

use App\Core\Controller;
use App\Models\Cerca;
use App\Models\Dispositivo;

class DispositivoController extends Controller {

    private function autenticar(): void {
        $header = $_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['HTTP_X_API_KEY'] ?? '';
        $token  = str_replace('Bearer ', '', $header);
        if (!hash_equals(API_SECRET, $token)) {
            $this->json(['erro' => 'Não autorizado.'], 401);
        }
    }

    /** POST /api/dispositivo/ping — chamado pelo app a cada abertura */
    public function ping(): void {
        $this->autenticar();
        $body       = json_decode(file_get_contents('php://input'), true) ?? [];
        $deviceId   = trim($body['device_id']   ?? '');
        $deviceNome = trim($body['device_nome']  ?? '');
        $appVersion = trim($body['app_version']  ?? '');
        $chave      = trim($body['license_key']  ?? '') ?: null;

        if (!$deviceId) {
            $this->json(['ok' => false, 'erro' => 'device_id obrigatório.'], 400);
        }

        (new Dispositivo())->ping($deviceId, $deviceNome, $appVersion, $chave);
        // Diz ao app se a empresa dele usa a cerca digital (versões antigas ignoram o campo)
        $ctx = (new Cerca())->contextoDoDispositivo($deviceId);
        $this->json(['ok' => true, 'rastreamento' => (bool)($ctx['rastreamento_ativo'] ?? false)]);
    }

    /**
     * POST /api/dispositivo/sinal — lote de sinais (roteador Wi-Fi, bateria, GPS opcional).
     * Só grava se o dispositivo pertence a uma empresa com a cerca digital ligada.
     */
    public function sinal(): void {
        $this->autenticar();
        $body     = json_decode(file_get_contents('php://input'), true) ?? [];
        $deviceId = trim((string)($body['device_id'] ?? ''));
        $sinais   = $body['sinais'] ?? null;

        if (!$deviceId || !is_array($sinais)) {
            $this->json(['ok' => false, 'erro' => 'device_id e sinais[] são obrigatórios.'], 400);
        }

        $cerca = new Cerca();
        $ctx   = $cerca->contextoDoDispositivo($deviceId);
        if (!$ctx || !(int)$ctx['rastreamento_ativo']) {
            $this->json(['ok' => true, 'rastreamento' => false, 'gravados' => 0]);
        }

        $gravados = $cerca->registrarSinais($ctx, $sinais, $_SERVER['REMOTE_ADDR'] ?? null);
        $this->json(['ok' => true, 'rastreamento' => true, 'gravados' => $gravados, 'intervalo_seg' => 120]);
    }
}
