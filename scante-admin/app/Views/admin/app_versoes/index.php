<?php if ($flash): ?>
<div class="alert alert-<?= $flash['type'] === 'success' ? 'success' : 'warning' ?> alert-dismissible fade show">
  <?= $flash['message'] ?><button type="button" class="btn-close" data-bs-dismiss="alert"></button>
</div>
<?php endif; ?>

<div class="d-flex align-items-center justify-content-between mb-4">
  <h4 class="fw-bold mb-0">Versões do app</h4>
  <button class="btn btn-accent" data-bs-toggle="modal" data-bs-target="#modalNovaVersao">
    <i class="bi bi-upload me-1"></i>Cadastrar versão
  </button>
</div>

<p class="text-muted small">
  Cada versão cadastrada aqui fica com um link de download fixo — o app verifica sozinho se tem
  uma versão mais nova (na mesma chamada que valida a licença) e avisa o usuário, que toca pra
  baixar e instalar. Pra dar a mesma versão pra todo mundo, deixe "Empresa exclusiva" em branco.
  Pra travar uma empresa numa versão específica (inclusive uma exclusiva dela), defina isso na
  página da empresa, em "Versão do app".
</p>

<div class="card">
  <div class="card-body p-0">
    <div class="table-responsive">
      <table class="table table-hover mb-0">
        <thead class="table-light">
          <tr>
            <th>Versão</th>
            <th>Code</th>
            <th>Changelog</th>
            <th>Empresa exclusiva</th>
            <th>Cadastrada em</th>
            <th>Link de download</th>
          </tr>
        </thead>
        <tbody>
        <?php foreach ($versoes as $v): ?>
          <tr>
            <td class="fw-semibold">v<?= htmlspecialchars($v['version_name']) ?></td>
            <td><code><?= (int)$v['version_code'] ?></code></td>
            <td class="small" style="max-width:360px; white-space:pre-wrap;"><?= htmlspecialchars($v['changelog']) ?></td>
            <td>
              <?php if ($v['empresa_nome']): ?>
                <span class="badge bg-warning text-dark"><?= htmlspecialchars($v['empresa_nome']) ?></span>
              <?php else: ?>
                <span class="text-muted small">Geral (qualquer empresa)</span>
              <?php endif; ?>
            </td>
            <td><?= date('d/m/Y H:i', strtotime($v['criada_em'])) ?></td>
            <td>
              <?php $url = APP_URL . '/downloads/apks/' . $v['arquivo_apk']; ?>
              <a href="<?= htmlspecialchars($url) ?>" target="_blank" class="small"><?= htmlspecialchars($url) ?></a>
            </td>
          </tr>
        <?php endforeach; ?>
        <?php if (empty($versoes)): ?>
          <tr><td colspan="6" class="text-center text-muted py-4">Nenhuma versão cadastrada ainda.</td></tr>
        <?php endif; ?>
        </tbody>
      </table>
    </div>
  </div>
</div>

<!-- Modal Nova Versão -->
<div class="modal fade" id="modalNovaVersao" tabindex="-1">
  <div class="modal-dialog">
    <div class="modal-content">
      <form method="POST" action="<?= APP_URL ?>/admin/app-versoes/criar" enctype="multipart/form-data">
        <div class="modal-header">
          <h5 class="modal-title">Cadastrar versão do app</h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
          <div class="row g-3">
            <div class="col-md-6">
              <label class="form-label">Nome da versão</label>
              <input type="text" name="version_name" class="form-control" placeholder="Ex: 1.1.2" required>
            </div>
            <div class="col-md-6">
              <label class="form-label">Version code <span class="text-muted fw-normal">(do build.gradle.kts)</span></label>
              <input type="number" name="version_code" class="form-control" placeholder="Ex: 5" required>
            </div>
            <div class="col-12">
              <label class="form-label">Changelog <span class="text-muted fw-normal">(o que mudou nessa versão)</span></label>
              <textarea name="changelog" class="form-control" rows="4" required
                placeholder="- Som de alerta personalizável no alarme do host&#10;- Múltiplas sessões com hosts diferentes"></textarea>
            </div>
            <div class="col-12">
              <label class="form-label">Empresa exclusiva <span class="text-muted fw-normal">(opcional — deixe em branco pra uma versão geral)</span></label>
              <select name="empresa_exclusiva_id" class="form-select">
                <option value="">— Geral, qualquer empresa —</option>
                <?php foreach ($empresas as $e): ?>
                  <option value="<?= $e['id'] ?>"><?= htmlspecialchars($e['nome']) ?></option>
                <?php endforeach; ?>
              </select>
            </div>
            <div class="col-12">
              <label class="form-label">Arquivo .apk</label>
              <input type="file" name="apk" accept=".apk" class="form-control" required>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button>
          <button type="submit" class="btn btn-accent">Cadastrar</button>
        </div>
      </form>
    </div>
  </div>
</div>
