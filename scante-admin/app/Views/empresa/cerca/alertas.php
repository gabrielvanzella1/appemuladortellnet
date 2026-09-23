<?php
use App\Models\Cerca;
$aba = 'alertas';
include __DIR__ . '/_nav.php';

$duracao = function (string $ini, ?string $fim) use ($agora): string {
    $s = ($fim ? Cerca::utcTs($fim) : $agora) - Cerca::utcTs($ini);
    if ($s < 3600) return max(1, intdiv($s, 60)) . ' min';
    return intdiv($s, 3600) . ' h ' . str_pad((string)intdiv($s % 3600, 60), 2, '0', STR_PAD_LEFT);
};
?>
<form method="get" class="d-flex flex-wrap gap-2 mb-3">
  <select name="tipo" class="form-select form-select-sm" style="width:auto">
    <option value="">Todos os tipos</option>
    <?php foreach ($tiposAlerta as $k => [$l]): ?>
      <option value="<?= $k ?>" <?= $tipo === $k ? 'selected' : '' ?>><?= $l ?></option>
    <?php endforeach; ?>
  </select>
  <select name="dias" class="form-select form-select-sm" style="width:auto">
    <?php foreach ([1 => 'Últimas 24 h', 7 => 'Últimos 7 dias', 30 => 'Últimos 30 dias', 90 => 'Últimos 90 dias'] as $k => $l): ?>
      <option value="<?= $k ?>" <?= $dias === $k ? 'selected' : '' ?>><?= $l ?></option>
    <?php endforeach; ?>
  </select>
  <button class="btn btn-sm btn-outline-secondary"><i class="bi bi-funnel me-1"></i>Filtrar</button>
</form>

<div class="card">
  <div class="table-responsive">
    <table class="table table-hover align-middle mb-0">
      <thead class="table-light">
        <tr><th class="ps-3">Alerta</th><th>Coletor</th><th>Início</th><th>Fim</th><th>Duração</th><th>Local</th><th>Detalhe</th></tr>
      </thead>
      <tbody>
      <?php foreach ($alertas as $a): ?>
        <tr>
          <td class="ps-3"><?= $badgeAlerta($a['tipo']) ?></td>
          <td><a href="<?= APP_URL ?>/empresa/cerca/dispositivo/<?= urlencode($a['device_id']) ?>?dia=<?= Cerca::local($a['inicio_em'], 'Y-m-d') ?>" class="text-decoration-none fw-semibold"><?= htmlspecialchars($a['device_nome'] ?: $a['device_id']) ?></a></td>
          <td style="font-size:.85rem"><?= Cerca::local($a['inicio_em'], 'd/m H:i') ?></td>
          <td style="font-size:.85rem"><?= $a['fim_em'] ? Cerca::local($a['fim_em'], 'd/m H:i') : '<span class="badge bg-danger">em aberto</span>' ?></td>
          <td style="font-size:.85rem"><?= $duracao($a['inicio_em'], $a['fim_em']) ?></td>
          <td style="font-size:.85rem"><?= $onde($a['zona'], null, $a['bssid']) ?></td>
          <td class="text-muted" style="font-size:.82rem"><?= htmlspecialchars($a['detalhe'] ?? '') ?></td>
        </tr>
      <?php endforeach; ?>
      <?php if (!$alertas): ?>
        <tr><td colspan="7" class="text-center text-muted py-5">Nenhum alerta no período.</td></tr>
      <?php endif; ?>
      </tbody>
    </table>
  </div>
</div>
<p class="text-muted mt-2" style="font-size:.8rem">
  "Sem comunicação" em andamento aparece na Visão geral; aqui ficam os períodos já encerrados (o coletor voltou).
</p>
