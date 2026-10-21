<?php
namespace App\Controllers\Admin;

use App\Core\Controller;
use App\Core\Auth;
use App\Models\AppVersao;
use App\Models\Empresa;

class AppVersoesController extends Controller {

    private const DIR_APKS = __DIR__ . '/../../../public/downloads/apks/';

    public function index(): void {
        Auth::requireAdmin();
        $this->view('admin.app_versoes.index', [
            'versoes'  => (new AppVersao())->listarComEmpresa(),
            'empresas' => (new Empresa())->findAll('nome ASC'),
            'flash'    => $this->getFlash(),
        ], 'admin');
    }

    public function criar(): void {
        Auth::requireAdmin();
        if (!$this->isPost()) { $this->redirect('/admin/app-versoes'); return; }

        $versionCode = (int)$this->input('version_code', '0');
        $versionName = trim($this->input('version_name', ''));
        $changelog   = trim($this->input('changelog', ''));
        $empresaExclusivaId = (int)$this->input('empresa_exclusiva_id', '0') ?: null;

        if ($versionCode <= 0 || !$versionName || !$changelog) {
            $this->flash('warning', 'Preencha o código da versão, o nome e o changelog.');
            $this->redirect('/admin/app-versoes');
            return;
        }

        if (empty($_FILES['apk']['tmp_name']) || !is_uploaded_file($_FILES['apk']['tmp_name'])) {
            $this->flash('warning', 'Selecione o arquivo .apk.');
            $this->redirect('/admin/app-versoes');
            return;
        }

        if (!is_dir(self::DIR_APKS)) mkdir(self::DIR_APKS, 0755, true);

        $nomeArquivo = 'ScanTE-' . $versionName . '-' . $versionCode . '.apk';
        $destino = self::DIR_APKS . $nomeArquivo;

        if (!move_uploaded_file($_FILES['apk']['tmp_name'], $destino)) {
            $this->flash('danger', 'Falha ao salvar o arquivo .apk no servidor.');
            $this->redirect('/admin/app-versoes');
            return;
        }

        try {
            (new AppVersao())->criar([
                'version_code'         => $versionCode,
                'version_name'         => $versionName,
                'changelog'            => $changelog,
                'empresa_exclusiva_id' => $empresaExclusivaId,
                'arquivo_apk'          => $nomeArquivo,
                'criado_por'           => Auth::id(),
            ]);
            $this->flash('success', 'Versão cadastrada. Link de download já disponível na lista.');
        } catch (\Throwable $e) {
            @unlink($destino);
            $motivo = str_contains($e->getMessage(), 'uniq_version_code')
                ? 'Já existe uma versão cadastrada com esse version_code.'
                : 'Erro: ' . $e->getMessage();
            $this->flash('danger', 'Não foi possível cadastrar a versão. ' . $motivo);
        }

        $this->redirect('/admin/app-versoes');
    }
}
