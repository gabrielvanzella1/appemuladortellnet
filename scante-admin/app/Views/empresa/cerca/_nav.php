<?php
use App\Models\Cerca;

// Rótulos compartilhados pelas telas da cerca
$estados = [
    'dentro'             => ['Dentro',               'success',   'bi-check-circle-fill'],
    'fora_da_cerca'      => ['Fora da cerca',        'warning',   'bi-exclamation-triangle-fill'],
    'sem_comunicacao'    => ['Sem comunicação',      'danger',    'bi-wifi-off'],
    'fora_do_expediente' => ['Fora do expediente',   'secondary', 'bi-moon'],
    'sem_cerca'          => ['Cerca não configurada','info',      'bi-question-circle'],
    'sem_localizacao'    => ['Sem localização',      'info',      'bi-geo'],
    'sem_dados'          => ['Sem dados',            'light',     'bi-dash-circle'],
];
$tiposAlerta = [
    'fora_da_cerca'   => ['Fora da cerca',   'warning', 'bi-exclamation-triangle-fill'],
    'sem_comunicacao' => ['Sem comunicação', 'danger',  'bi-wifi-off'],
    'bateria_baixa'   => ['Bateria baixa',   'dark',    'bi-battery-half'],
];
$badgeEstado = function (string $e) use ($estados): string {
    [$l, $c, $i] = $estados[$e] ?? ['—', 'secondary', 'bi-dot'];
    $txt = $c === 'warning' || $c === 'light' || $c === 'info' ? 'text-dark' : '';
    return "<span class=\"badge bg-$c $txt\"><i class=\"bi $i me-1\"></i>$l</span>";
};
$badgeAlerta = function (string $t) use ($tiposAlerta): string {
    [$l, $c, $i] = $tiposAlerta[$t] ?? ['—', 'secondary', 'bi-dot'];
    $txt = $c === 'warning' ? 'text-dark' : '';
    return "<span class=\"badge bg-$c $txt\"><i class=\"bi $i me-1\"></i>$l</span>";
};
// Onde o coletor está: zona (se nomeada) ou a rede/roteador
$onde = function (?string $zona, ?string $ssid, ?string $bssid): string {
    if ($zona) return '<i class="bi bi-geo-alt-fill text-success me-1"></i>' . htmlspecialchars($zona);
    if ($bssid) return '<span class="text-muted">' . htmlspecialchars($ssid ?? 'Wi-Fi') . ' · <code style="font-size:.75rem">' . $bssid . '</code></span>';
    return '<span class="text-muted">—</span>';
};
$abas = [
    'index'  => ['Visão geral',       '/empresa/cerca',         'bi-grid'],
    'pontos' => ['Pontos de acesso',  '/empresa/cerca/pontos',  'bi-router'],
    'alertas'=> ['Alertas',           '/empresa/cerca/alertas', 'bi-bell'],
    'config' => ['Configurações',     '/empresa/cerca/config',  'bi-sliders'],
];
?>
<?php if ($flash): ?>
<div class="alert alert-<?= $flash['type'] === 'success' ? 'success' : 'danger' ?> alert-dismissible fade show">
  <?= $flash['message'] ?><button type="button" class="btn-close" data-bs-dismiss="alert"></button>
</div>
<?php endif; ?>

<div class="d-flex align-items-center justify-content-between mb-3">
  <div>
    <h4 class="fw-bold mb-0"><i class="bi bi-bounding-box-circles me-2"></i>Cerca digital</h4>
    <small class="text-muted">A rede Wi-Fi do CD funciona como cerca — sem GPS.</small>
  </div>
  <small class="text-muted">Atualizado <?= Cerca::local(gmdate('Y-m-d H:i:s', $agora), 'H:i') ?></small>
</div>

<ul class="nav nav-tabs mb-4">
<?php foreach ($abas as $k => [$label, $url, $icon]): ?>
  <li class="nav-item">
    <a class="nav-link <?= ($aba ?? '') === $k ? 'active fw-semibold' : '' ?>" href="<?= APP_URL . $url ?>">
      <i class="bi <?= $icon ?> me-1"></i><?= $label ?>
    </a>
  </li>
<?php endforeach; ?>
</ul>
