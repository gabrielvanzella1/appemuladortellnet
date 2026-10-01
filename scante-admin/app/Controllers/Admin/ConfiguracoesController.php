<?php
namespace App\Controllers\Admin;

use App\Core\Controller;
use App\Core\Auth;
use App\Models\Configuracao;

class ConfiguracoesController extends Controller {

    private array $chaves = ['email_notificacoes'];

    public function index(): void {
        Auth::requireAdmin();

        $cfg    = new Configuracao();
        $dados  = $cfg->getMultiple($this->chaves);
        if (empty($dados['email_notificacoes'])) {
            $dados['email_notificacoes'] = 'scante@scante.com.br';
        }

        $this->view('admin.configuracoes.index', [
            'cfg'    => $dados,
            'flash'  => $this->getFlash(),
        ], 'admin');
    }

    public function salvar(): void {
        Auth::requireAdmin();

        $cfg = new Configuracao();

        // E-mail que recebe aviso de novas solicitações de licenças
        $val = trim($this->input('email_notificacoes', ''));
        if ($val) {
            $cfg->set('email_notificacoes', $val);
        }

        $this->flash('success', 'Configurações salvas.');
        $this->redirect('/admin/configuracoes');
    }
}
