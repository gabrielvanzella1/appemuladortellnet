<?php
use App\Models\Cerca;
$aba = 'pontos';
include __DIR__ . '/_nav.php';
?>
<div class="alert alert-light border d-flex gap-2 align-items-start" style="font-size:.9rem">
  <i class="bi bi-lightbulb text-warning fs-5"></i>
  <div>
    Estes são os roteadores Wi-Fi em que os seus coletores já se conectaram — eles aparecem aqui sozinhos.
    <strong>Marque os que ficam dentro do CD</strong> (eles formam a cerca) e dê um <strong>nome de zona</strong>
    para cada um (ex.: <em>Doca 1</em>, <em>Recebimento</em>, <em>Estoque</em>). Coletor conectado a um roteador
    fora da cerca gera alerta; coletor que some da rede aparece como "sem comunicação".
  </div>
</div>

<form method="post" action="<?= APP_URL ?>/empresa/cerca/pontos/salvar">
<div class="card">
  <div class="table-responsive">
    <table class="table align-middle mb-0">
      <thead class="table-light">
        <tr>
          <th class="ps-3" style="width:110px">Na cerca</th>
          <th>Zona</th>
          <th>Rede (SSID)</th>
          <th>Roteador (BSSID)</th>
          <th class="text-center">Coletores agora</th>
          <th>Visto por último</th>
        </tr>
      </thead>
      <tbody>
      <?php foreach ($pontos as $p): ?>
        <tr class="<?= $p['na_cerca'] ? '' : 'table-light' ?>">
          <td class="ps-3">
            <div class="form-check form-switch">
              <input class="form-check-input" type="checkbox" name="na_cerca[<?= $p['id'] ?>]" value="1" <?= $p['na_cerca'] ? 'checked' : '' ?>>
            </div>
          </td>
          <td style="min-width:200px">
            <input type="text" name="zona[<?= $p['id'] ?>]" class="form-control form-control-sm" maxlength="100"
                   placeholder="Ex.: Doca 1" value="<?= htmlspecialchars($p['zona'] ?? '') ?>">
          </td>
          <td><?= htmlspecialchars($p['ssid'] ?? '—') ?></td>
          <td><code style="font-size:.8rem"><?= $p['bssid'] ?></code></td>
          <td class="text-center"><?= (int)$p['coletores_agora'] ? '<span class="badge bg-success">' . (int)$p['coletores_agora'] . '</span>' : '<span class="text-muted">0</span>' ?></td>
          <td class="text-muted" style="font-size:.85rem"><?= Cerca::ha($p['ultimo_visto'], $agora) ?></td>
        </tr>
      <?php endforeach; ?>
      <?php if (!$pontos): ?>
        <tr><td colspan="6" class="text-center text-muted py-5">
          Nenhum roteador descoberto ainda.<br>Eles aparecem aqui assim que os coletores começarem a enviar sinais.
        </td></tr>
      <?php endif; ?>
      </tbody>
    </table>
  </div>
</div>
<?php if ($pontos): ?>
<div class="text-end mt-3">
  <button type="submit" class="btn btn-accent"><i class="bi bi-check-lg me-1"></i> Salvar cerca</button>
</div>
<?php endif; ?>
</form>
