<?php
namespace App\Controllers\Api;

use App\Core\Controller;
use App\Models\Licenca;

class LicencaController extends Controller {

    // Autenticação simples via header Authorization: Bearer <API_SECRET>
    private function autenticar(): void {
        $header = $_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['HTTP_X_API_KEY'] ?? '';
        $token  = str_replace('Bearer ', '', $header);
        if (!hash_equals(API_SECRET, $token)) {
            $this->json(['erro' => 'Não autorizado.'], 401);
        }
    }

    /** POST /api/licenca/validar — chamado pelo app Android ao abrir */
    public function validar(): void {
        $this->autenticar();
        $body      = json_decode(file_get_contents('php://input'), true) ?? [];
        $chave     = trim($body['chave'] ?? '');
        $deviceId  = trim($body['device_id'] ?? '');
        $deviceNome = trim($body['device_nome'] ?? '');

        if (!$chave || !$deviceId) {
            $this->json(['valida' => false, 'mensagem' => 'Parâmetros incompletos.'], 400);
        }

        $resultado = (new Licenca())->validarParaApp($chave, $deviceId, $deviceNome);
        $this->json($resultado, $resultado['valida'] ? 200 : 403);
    }

}
