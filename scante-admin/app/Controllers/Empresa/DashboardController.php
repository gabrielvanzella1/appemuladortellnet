<?php
namespace App\Controllers\Empresa;

use App\Core\Controller;
use App\Core\Auth;
use App\Models\Licenca;
use App\Models\Empresa;

class DashboardController extends Controller {

    public function index(): void {
        Auth::requireEmpresa();
        $empresaId = Auth::empresaId();
        $empresa   = (new Empresa())->findById($empresaId);
        $licencas  = (new Licenca())->findByEmpresa($empresaId);

        $ativas    = array_filter($licencas, fn($l) => $l['status'] === 'ativa');
        $trial     = array_filter($licencas, fn($l) => $l['status'] === 'trial');
        $expiradas = array_filter($licencas, fn($l) => $l['status'] === 'expirada');

        $this->view('empresa.dashboard', [
            'empresa'   => $empresa,
            'licencas'  => $licencas,
            'ativas'    => count($ativas),
            'trial'     => count($trial),
            'expiradas' => count($expiradas),
            'flash'     => $this->getFlash(),
        ], 'empresa');
    }

    /** Liga/desliga e configura o bloqueio de edição de conexão (usuário/senha) da própria empresa. */
    public function lockConexao(): void {
        Auth::requireEmpresa();
        $empresaId = Auth::empresaId();

        if ($this->isPost()) {
            (new Empresa())->salvarLockConexao(
                $empresaId,
                $this->input('lock_conexao_ativo', '0') === '1',
                $this->sanitize($this->input('lock_conexao_usuario', '')),
                $this->input('lock_conexao_senha', '')
            );
            $this->flash('success', 'Bloqueio de edição de conexão salvo.');
        }
        $this->redirect('/empresa');
    }
}
