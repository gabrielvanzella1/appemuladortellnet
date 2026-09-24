<?php
namespace App\Controllers\Empresa;

use App\Core\Controller;
use App\Core\Auth;
use App\Models\Cerca;
use DateTimeImmutable;
use DateTimeZone;

/** Cerca digital por Wi-Fi — painel da empresa. Só abre com o rastreamento ligado pelo admin. */
class CercaController extends Controller {

    private Cerca $cerca;
    private array $empresa;

    private function carregar(): void {
        Auth::requireEmpresa();
        $this->cerca = new Cerca();
        $emp = Auth::empresaId() ? $this->cerca->empresa((int)Auth::empresaId()) : null;
        if (!$emp || !(int)$emp['rastreamento_ativo']) {
            $this->flash('error', 'A cerca digital não está ativada para a sua empresa. Fale com a ScanTE.');
            $this->redirect('/empresa');
        }
        $this->empresa = $emp;
    }

    private function render(string $view, array $data): void {
        $this->view('empresa.cerca.' . $view, $data + [
            'empresa' => $this->empresa,
            'flash'   => $this->getFlash(),
            'agora'   => time(),
        ], 'empresa');
    }

    public function index(): void {
        $this->carregar();
        $disp = $this->cerca->dispositivos($this->empresa);
        $this->render('index', [
            'dispositivos' => $disp,
            'alertas'      => $this->cerca->alertasAbertos($this->empresa, $disp),
            'temCerca'     => (bool)$this->cerca->cercaDaEmpresa((int)$this->empresa['id']),
        ]);
    }

    public function pontos(): void {
        $this->carregar();
        $this->render('pontos', ['pontos' => $this->cerca->pontos($this->empresa)]);
    }

    public function salvarPontos(): void {
        $this->carregar();
        if ($this->isPost()) {
            $naCerca = is_array($_POST['na_cerca'] ?? null) ? $_POST['na_cerca'] : [];
            $zonas   = is_array($_POST['zona'] ?? null) ? $_POST['zona'] : [];
            $this->cerca->salvarPontos((int)$this->empresa['id'], $naCerca, $zonas);
            $this->flash('success', 'Pontos de acesso salvos. A cerca passa a valer para os próximos sinais.');
        }
        $this->redirect('/empresa/cerca/pontos');
    }

    public function alertas(): void {
        $this->carregar();
        $tipo = $this->input('tipo', '');
        $tipo = in_array($tipo, ['fora_da_cerca', 'sem_comunicacao', 'bateria_baixa'], true) ? $tipo : null;
        $dias = (int)$this->input('dias', 7);
        $dias = in_array($dias, [1, 7, 30, 90], true) ? $dias : 7;
        $this->render('alertas', [
            'alertas' => $this->cerca->alertas((int)$this->empresa['id'], $tipo, $dias),
            'tipo'    => $tipo,
            'dias'    => $dias,
        ]);
    }

    public function dispositivo(string $deviceId): void {
        $this->carregar();
        $deviceId = urldecode($deviceId);
        $disp = $this->cerca->dispositivoDaEmpresa((int)$this->empresa['id'], $deviceId);
        if (!$disp) {
            $this->redirect('/empresa/cerca');
        }
        $hoje = (new DateTimeImmutable('now', new DateTimeZone(Cerca::TZ)))->format('Y-m-d');
        $dia  = (string)$this->input('dia', $hoje);
        if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $dia)) $dia = $hoje;

        $lista = $this->cerca->dispositivos($this->empresa);
        $atual = array_values(array_filter($lista, fn($d) => $d['device_id'] === $deviceId))[0] ?? $disp;
        $this->render('dispositivo', [
            'disp'    => $atual,
            'dia'     => $dia,
            'hoje'    => $hoje,
            'trechos' => $this->cerca->linhaDoTempo($this->empresa, $deviceId, $dia),
        ]);
    }

    public function config(): void {
        $this->carregar();
        if ($this->isPost()) {
            $hora = fn($v, $pad) => preg_match('/^([01]\d|2[0-3]):[0-5]\d$/', (string)$v) ? $v . ':00' : $pad;
            $dias = array_values(array_intersect(array_map('intval', (array)($_POST['dias'] ?? [])), [1, 2, 3, 4, 5, 6, 7]));
            sort($dias);
            if (!$dias) {
                $this->flash('error', 'Escolha pelo menos um dia de expediente.');
                $this->redirect('/empresa/cerca/config');
            }
            $this->cerca->salvarConfig((int)$this->empresa['id'], [
                'limite_sem_comunicacao_min' => max(5, min(240, (int)$this->input('limite_sem_comunicacao_min', 15))),
                'limite_bateria_pct'         => max(5, min(50, (int)$this->input('limite_bateria_pct', 15))),
                'expediente_inicio'          => $hora($this->input('expediente_inicio'), '06:00:00'),
                'expediente_fim'             => $hora($this->input('expediente_fim'), '22:00:00'),
                'expediente_dias'            => implode(',', $dias),
            ]);
            $this->flash('success', 'Configurações salvas.');
            $this->redirect('/empresa/cerca/config');
        }
        $this->render('config', []);
    }
}
