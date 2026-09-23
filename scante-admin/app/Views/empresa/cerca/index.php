<?php
use App\Models\Cerca;
$aba = 'index';
include __DIR__ . '/_nav.php';

$conta = array_count_values(array_column($dispositivos, 'estado'));
$porZona = [];
foreach ($dispositivos as $d) {
    if ($d['estado'] === 'dentro') $porZona[$d['zona'] ?: 'Sem nome'][] = $d;
}
ksort($porZona);
$cards = [
    ['Dentro da cerca', $conta['dentro'] ?? 0,          'success', 'bi-check-circle-fill'],
    ['Fora da cerca',   $conta['fora_da_cerca'] ?? 0,   'warning', 'bi-exclamation-triangle-fill'],
    ['Sem comunicação', $conta['sem_comunicacao'] ?? 0, 'danger',  'bi-wifi-off'],
    ['Coletores',       count($dispositivos),           'primary', 'bi-phone'],
];
?>

<?php if (!$temCerca): ?>
<div class="alert alert-info d-flex align-items-start gap-2">
  <i class="bi bi-info-circle-fill fs-5"></i>
  <div>
    <strong>A cerca ainda não foi configurada.</strong> Os roteadores aparecem sozinhos em
    <a href="<?= APP_URL ?>/empresa/cerca/pontos">Pontos de acesso</a> conforme os coletores se conectam.
    Marque os que são do CD e dê um nome para cada zona.
  </div>
</div>
<?php endif; ?>

<div class="row g-3 mb-4">
<?php foreach ($cards as [$label, $n, $cor, $icon]): ?>
  <div class="col-6 col-lg-3">
    <div class="card h-100">
      <div class="card-body d-flex align-items-center gap-3">
        <div class="rounded-circle bg-<?= $cor ?> bg-opacity-10 text-<?= $cor ?> d-flex align-items-center justify-content-center" style="width:46px;height:46px">
          <i class="bi <?= $icon ?> fs-4"></i>
        </div>
        <div>
          <div class="fs-3 fw-bold lh-1"><?= $n ?></div>
          <small class="text-muted"><?= $label ?></small>
        </div>
      </div>
    </div>
  </div>
<?php endforeach; ?>
</div>

<div class="card mb-4">
  <div class="card-body">
    <h6 class="fw-bold mb-3"><i class="bi bi-bell-fill me-2 text-danger"></i>Alertas abertos
      <span class="badge bg-secondary ms-1"><?= count($alertas) ?></span></h6>
    <?php if (!$alertas): ?>
      <p class="text-muted mb-0">Nenhum alerta aberto. Tudo dentro da cerca.</p>
    <?php else: ?>
    <div class="table-responsive">
      <table class="table table-sm align-middle mb-0">
        <thead class="table-light"><tr><th>Alerta</th><th>Coletor</th><th>Desde</th><th>Último local</th><th>Detalhe</th></tr></thead>
        <tbody>
        <?php foreach ($alertas as $a): ?>
          <tr>
            <td><?= $badgeAlerta($a['tipo']) ?></td>
            <td><a href="<?= APP_URL ?>/empresa/cerca/dispositivo/<?= urlencode($a['device_id']) ?>" class="fw-semibold text-decoration-none"><?= htmlspecialchars($a['device_nome'] ?: $a['device_id']) ?></a></td>
            <td style="font-size:.85rem"><?= Cerca::local($a['inicio_em'], 'd/m H:i') ?> <span class="text-muted">(<?= Cerca::ha($a['inicio_em'], $agora) ?>)</span></td>
            <td style="font-size:.85rem"><?= $onde($a['zona'] ?? null, null, $a['bssid']) ?></td>
            <td class="text-muted" style="font-size:.82rem"><?= htmlspecialchars($a['detalhe'] ?? '') ?></td>
          </tr>
        <?php endforeach; ?>
        </tbody>
      </table>
    </div>
    <?php endif; ?>
  </div>
</div>

<?php if ($porZona): ?>
<h6 class="fw-bold mb-3"><i class="bi bi-geo-alt-fill me-2 text-success"></i>Coletores por zona</h6>
<div class="row g-3 mb-4">
<?php foreach ($porZona as $zona => $lista): ?>
  <div class="col-md-4 col-lg-3">
    <div class="card h-100 border-start border-4 border-success">
      <div class="card-body">
        <div class="d-flex justify-content-between align-items-center mb-2">
          <span class="fw-bold"><?= htmlspecialchars($zona) ?></span>
          <span class="badge bg-success"><?= count($lista) ?></span>
        </div>
        <?php foreach ($lista as $d): ?>
          <div style="font-size:.82rem" class="text-muted text-truncate"><i class="bi bi-phone me-1"></i><?= htmlspecialchars($d['device_nome'] ?: $d['device_id']) ?></div>
        <?php endforeach; ?>
      </div>
    </div>
  </div>
<?php endforeach; ?>
</div>
<?php endif; ?>

<div class="card">
  <div class="card-body">
    <h6 class="fw-bold mb-3"><i class="bi bi-phone me-2"></i>Coletores</h6>
    <div class="table-responsive">
      <table class="table table-hover align-middle mb-0">
        <thead class="table-light"><tr><th>Coletor</th><th>Estado</th><th>Onde está</th><th>Último sinal</th><th>Bateria</th><th></th></tr></thead>
        <tbody>
        <?php foreach ($dispositivos as $d): ?>
          <tr>
            <td class="fw-semibold"><?= htmlspecialchars($d['device_nome'] ?: $d['device_id']) ?></td>
            <td><?= $badgeEstado($d['estado']) ?></td>
            <td style="font-size:.85rem"><?= $onde($d['zona'], $d['ultimo_ssid'], $d['ultimo_bssid']) ?></td>
            <td class="text-muted" style="font-size:.85rem" title="<?= Cerca::local($d['ultimo_sinal_em'], 'd/m/Y H:i:s') ?>"><?= Cerca::ha($d['ultimo_sinal_em'], $agora) ?></td>
            <td style="font-size:.85rem">
              <?php if ($d['ultima_bateria_pct'] !== null): $b = (int)$d['ultima_bateria_pct']; ?>
                <i class="bi <?= $d['ultimo_carregando'] ? 'bi-battery-charging text-success' : ($b < (int)$empresa['limite_bateria_pct'] ? 'bi-battery text-danger' : 'bi-battery-half') ?>"></i> <?= $b ?>%
              <?php else: ?><span class="text-muted">—</span><?php endif; ?>
            </td>
            <td class="text-end"><a href="<?= APP_URL ?>/empresa/cerca/dispositivo/<?= urlencode($d['device_id']) ?>" class="btn btn-sm btn-outline-secondary"><i class="bi bi-clock-history me-1"></i>Linha do tempo</a></td>
          </tr>
        <?php endforeach; ?>
        <?php if (!$dispositivos): ?>
          <tr><td colspan="6" class="text-center text-muted py-4">Nenhum coletor desta empresa enviou sinal ainda.</td></tr>
        <?php endif; ?>
        </tbody>
      </table>
    </div>
  </div>
</div>

<script>setTimeout(() => location.reload(), 60000); // atualiza sozinho a cada minuto</script>
