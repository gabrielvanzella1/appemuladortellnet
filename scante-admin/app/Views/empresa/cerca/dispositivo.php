<?php
use App\Models\Cerca;
$aba = '';
include __DIR__ . '/_nav.php';

$hm  = fn(int $ts) => Cerca::local(gmdate('Y-m-d H:i:s', $ts), 'H:i');
$dur = function (int $s): string {
    if ($s < 60) return 'menos de 1 min';
    if ($s < 3600) return intdiv($s, 60) . ' min';
    return intdiv($s, 3600) . ' h ' . str_pad((string)intdiv($s % 3600, 60), 2, '0', STR_PAD_LEFT);
};
?>
<div class="d-flex flex-wrap align-items-center gap-2 mb-3">
  <a href="<?= APP_URL ?>/empresa/cerca" class="btn btn-sm btn-outline-secondary"><i class="bi bi-arrow-left"></i></a>
  <h5 class="fw-bold mb-0"><i class="bi bi-phone me-1"></i><?= htmlspecialchars($disp['device_nome'] ?: $disp['device_id']) ?></h5>
  <?= isset($disp['estado']) ? $badgeEstado($disp['estado']) : '' ?>
  <form method="get" class="ms-auto d-flex gap-2">
    <input type="date" name="dia" value="<?= $dia ?>" max="<?= $hoje ?>" class="form-control form-control-sm">
    <button class="btn btn-sm btn-outline-secondary text-nowrap">Ver dia</button>
  </form>
</div>

<div class="card">
  <div class="card-body">
    <h6 class="fw-bold mb-3"><i class="bi bi-clock-history me-2"></i>Por onde passou em <?= date('d/m/Y', strtotime($dia)) ?></h6>
    <?php if (!$trechos): ?>
      <p class="text-muted mb-0">Nenhum sinal neste dia.</p>
    <?php else: ?>
    <ul class="list-unstyled mb-0">
    <?php foreach ($trechos as $t): ?>
      <?php if ($t['tipo'] === 'sem_sinal'): ?>
        <li class="d-flex gap-3 py-2 border-bottom">
          <span class="text-danger fw-semibold" style="width:110px"><?= $hm($t['inicio_ts']) ?> – <?= $hm($t['fim_ts']) ?></span>
          <span class="text-danger"><i class="bi bi-wifi-off me-1"></i>Sem sinal por <?= $dur($t['fim_ts'] - $t['inicio_ts']) ?></span>
        </li>
      <?php else: ?>
        <li class="d-flex flex-wrap gap-3 py-2 border-bottom align-items-center">
          <span class="fw-semibold" style="width:110px"><?= $hm($t['inicio_ts']) ?> – <?= $hm($t['fim_ts']) ?></span>
          <span style="min-width:220px"><?= $onde($t['zona'], $t['ssid'], $t['bssid']) ?></span>
          <?= $t['bssid'] && $t['na_cerca']
              ? '<span class="badge bg-success">na cerca</span>'
              : '<span class="badge bg-warning text-dark">fora da cerca</span>' ?>
          <span class="text-muted" style="font-size:.82rem"><?= $dur($t['fim_ts'] - $t['inicio_ts']) ?> · <?= $t['sinais'] ?> sinal(is)</span>
          <?php if ($t['bateria'] !== null): ?><span class="text-muted" style="font-size:.82rem"><i class="bi bi-battery-half"></i> <?= $t['bateria'] ?>%</span><?php endif; ?>
        </li>
      <?php endif; ?>
    <?php endforeach; ?>
    </ul>
    <?php endif; ?>
  </div>
</div>
