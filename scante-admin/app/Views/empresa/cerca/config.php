<?php
$aba = 'config';
include __DIR__ . '/_nav.php';
$diasSel = array_map('intval', explode(',', $empresa['expediente_dias']));
$nomes = [1 => 'Seg', 2 => 'Ter', 3 => 'Qua', 4 => 'Qui', 5 => 'Sex', 6 => 'Sáb', 7 => 'Dom'];
?>
<form method="post" action="<?= APP_URL ?>/empresa/cerca/config" style="max-width:640px">
  <div class="card mb-3">
    <div class="card-body">
      <h6 class="fw-bold mb-3"><i class="bi bi-bell me-2"></i>Alertas</h6>
      <div class="row g-3">
        <div class="col-sm-6">
          <label class="form-label">Sem comunicação após</label>
          <div class="input-group">
            <input type="number" name="limite_sem_comunicacao_min" class="form-control" min="5" max="240" value="<?= (int)$empresa['limite_sem_comunicacao_min'] ?>">
            <span class="input-group-text">min</span>
          </div>
          <div class="form-text">Tempo sem sinal para o coletor ser considerado fora do CD.</div>
        </div>
        <div class="col-sm-6">
          <label class="form-label">Bateria baixa abaixo de</label>
          <div class="input-group">
            <input type="number" name="limite_bateria_pct" class="form-control" min="5" max="50" value="<?= (int)$empresa['limite_bateria_pct'] ?>">
            <span class="input-group-text">%</span>
          </div>
          <div class="form-text">Só alerta se o coletor não estiver carregando.</div>
        </div>
      </div>
    </div>
  </div>

  <div class="card mb-3">
    <div class="card-body">
      <h6 class="fw-bold mb-3"><i class="bi bi-clock me-2"></i>Expediente</h6>
      <p class="text-muted" style="font-size:.85rem">Fora do expediente, coletor sem sinal não gera alerta (ex.: guardado à noite).</p>
      <div class="row g-3 mb-3">
        <div class="col-6">
          <label class="form-label">Início</label>
          <input type="time" name="expediente_inicio" class="form-control" value="<?= substr($empresa['expediente_inicio'], 0, 5) ?>">
        </div>
        <div class="col-6">
          <label class="form-label">Fim</label>
          <input type="time" name="expediente_fim" class="form-control" value="<?= substr($empresa['expediente_fim'], 0, 5) ?>">
        </div>
      </div>
      <label class="form-label d-block">Dias</label>
      <?php foreach ($nomes as $n => $label): ?>
        <div class="form-check form-check-inline">
          <input class="form-check-input" type="checkbox" name="dias[]" value="<?= $n ?>" id="dia<?= $n ?>" <?= in_array($n, $diasSel, true) ? 'checked' : '' ?>>
          <label class="form-check-label" for="dia<?= $n ?>"><?= $label ?></label>
        </div>
      <?php endforeach; ?>
      <div class="form-text">Turno que vira a noite (ex.: 22:00 às 06:00) é aceito.</div>
    </div>
  </div>

  <button type="submit" class="btn btn-accent"><i class="bi bi-check-lg me-1"></i> Salvar</button>
</form>
