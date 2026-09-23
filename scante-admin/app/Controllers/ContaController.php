<?php
namespace App\Controllers;

use App\Core\Controller;
use App\Core\Auth;
use App\Models\Usuario;

/** Minha conta — troca de senha do próprio usuário (admin geral ou empresa). */
class ContaController extends Controller {

    public function senha(): void {
        Auth::requireEmpresa(); // qualquer usuário logado
        $layout = Auth::isAdmin() ? 'admin' : 'empresa';
        $erro = null;

        if ($this->isPost()) {
            $atual = (string)$this->input('senha_atual', '');
            $nova  = (string)$this->input('senha_nova', '');
            $conf  = (string)$this->input('senha_confirmacao', '');
            $model = new Usuario();
            $user  = $model->findById((int)Auth::id());

            if (!$user || !$model->verificarSenha($atual, $user['senha'])) {
                $erro = 'A senha atual está incorreta.';
            } elseif (strlen($nova) < 8) {
                $erro = 'A nova senha precisa ter pelo menos 8 caracteres.';
            } elseif ($nova !== $conf) {
                $erro = 'A confirmação não é igual à nova senha.';
            } elseif ($nova === $atual) {
                $erro = 'A nova senha precisa ser diferente da atual.';
            } else {
                $model->update((int)Auth::id(), ['senha' => $nova]);
                $this->flash('success', 'Senha alterada com sucesso.');
                $this->redirect('/conta/senha');
            }
        }

        $this->view('conta.senha', ['erro' => $erro, 'flash' => $this->getFlash()], $layout);
    }
}
