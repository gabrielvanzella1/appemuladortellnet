<?php if ($flash): ?>
<div class="alert alert-<?= $flash['type'] === 'success' ? 'success' : 'warning' ?> alert-dismissible fade show">
  <?= $flash['message'] ?><button type="button" class="btn-close" data-bs-dismiss="alert"></button>
</div>
<?php endif; ?>

<div class="d-flex align-items-center mb-4 gap-2">
  <a href="<?= APP_URL ?>/admin/empresas" class="btn btn-sm btn-outline-secondary">
    <i class="bi bi-arrow-left"></i>
  </a>
  <h4 class="fw-bold mb-0"><?= htmlspecialchars($empresa['nome']) ?></h4>
  <a href="<?= APP_URL ?>/admin/empresas/<?= $empresa['id'] ?>/editar" class="btn btn-sm btn-outline-primary ms-auto">
    <i class="bi bi-pencil me-1"></i>Editar
  </a>
</div>

<div class="row g-3 mb-4">
  <!-- Info da empresa -->
  <div class="col-md-4">
    <div class="card h-100">
      <div class="card-body">
        <h6 class="fw-bold mb-3"><i class="bi bi-building me-2"></i>Dados da Empresa</h6>
        <dl class="mb-0" style="font-size:.88rem">
          <dt class="text-muted">CNPJ</dt>
          <dd><?= htmlspecialchars($empresa['cnpj'] ?? '—') ?></dd>
          <dt class="text-muted">E-mail</dt>
          <dd><?= htmlspecialchars($empresa['email']) ?></dd>
          <dt class="text-muted">Telefone</dt>
          <dd><?= htmlspecialchars($empresa['telefone'] ?? '—') ?></dd>
          <dt class="text-muted">Contato</dt>
          <dd><?= htmlspecialchars($empresa['contato'] ?? '—') ?></dd>
          <dt class="text-muted">Status</dt>
          <dd><?= $empresa['ativo'] ? '<span class="badge bg-success">Ativa</span>' : '<span class="badge bg-secondary">Inativa</span>' ?></dd>
        </dl>
      </div>
    </div>
  </div>

  <!-- Usuários de acesso -->
  <div class="col-md-8">
    <div class="card h-100">
      <div class="card-body">
        <h6 class="fw-bold mb-3"><i class="bi bi-people me-2"></i>Usuários de Acesso</h6>
        <table class="table table-sm mb-0">
          <thead class="table-light">
            <tr><th>Nome</th><th>E-mail</th><th>Status</th></tr>
          </thead>
          <tbody>
          <?php foreach ($usuarios as $u): ?>
            <tr>
              <td><?= htmlspecialchars($u['nome']) ?></td>
              <td><?= htmlspecialchars($u['email']) ?></td>
              <td><?= $u['ativo'] ? '<span class="badge bg-success">Ativo</span>' : '<span class="badge bg-secondary">Inativo</span>' ?></td>
            </tr>
          <?php endforeach; ?>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</div>

<!-- Cerca digital (rastreamento por Wi-Fi) -->
<?php $rastreando = !empty($empresa['rastreamento_ativo']); ?>
<div class="card mb-3">
  <div class="card-body d-flex flex-wrap align-items-center gap-3">
    <div class="me-auto">
      <h6 class="fw-bold mb-1"><i class="bi bi-bounding-box-circles me-2"></i>Cerca digital (Wi-Fi)
        <span class="badge <?= $rastreando ? 'bg-success' : 'bg-secondary' ?> ms-1"><?= $rastreando ? 'Ativa' : 'Desativada' ?></span>
      </h6>
      <small class="text-muted">Rastreia os coletores pela rede Wi-Fi do CD (sem GPS). Quando ativa, o menu
        "Cerca digital" aparece no painel da empresa.</small>
    </div>
    <form method="post" action="<?= APP_URL ?>/admin/empresas/<?= $empresa['id'] ?>/rastreamento">
      <input type="hidden" name="ativo" value="<?= $rastreando ? 0 : 1 ?>">
      <button class="btn btn-sm <?= $rastreando ? 'btn-outline-danger' : 'btn-accent' ?>">
        <i class="bi <?= $rastreando ? 'bi-toggle-off' : 'bi-toggle-on' ?> me-1"></i><?= $rastreando ? 'Desativar' : 'Ativar' ?>
      </button>
    </form>
  </div>
</div>

<!-- Bloqueio de edição de conexão — só na empresa de teste interna (id 4, Scan TE Produção).
     Empresas de cliente configuram isso sozinhas em Empresa > Aparência. -->
<?php if ((int)$empresa['id'] === 4): ?>
<div class="card mb-3">
  <div class="card-body">
    <h6 class="fw-bold mb-1"><i class="bi bi-lock me-2"></i>Bloqueio de edição de conexão</h6>
    <small class="text-muted d-block mb-3">Quando ativado, editar uma conexão já salva no coletor pede esse usuário/senha.
      Criar a primeira conexão continua livre.</small>
    <form method="post" action="<?= APP_URL ?>/admin/empresas/<?= $empresa['id'] ?>/lock-conexao">
      <div class="form-check form-switch mb-3">
        <input class="form-check-input" type="checkbox" id="lock_conexao_ativo" name="lock_conexao_ativo" value="1"
               <?= !empty($empresa['lock_conexao_ativo']) ? 'checked' : '' ?>>
        <label class="form-check-label" for="lock_conexao_ativo">Exigir usuário/senha pra editar conexão</label>
      </div>
      <div class="row g-3 mb-3">
        <div class="col-md-6">
          <label class="form-label" style="font-size:.82rem">Usuário</label>
          <input type="text" class="form-control" name="lock_conexao_usuario" maxlength="100"
                 value="<?= htmlspecialchars($empresa['lock_conexao_usuario'] ?? '') ?>">
        </div>
        <div class="col-md-6">
          <label class="form-label" style="font-size:.82rem">Senha</label>
          <input type="text" class="form-control" name="lock_conexao_senha" maxlength="100"
                 value="<?= htmlspecialchars($empresa['lock_conexao_senha'] ?? '') ?>">
        </div>
      </div>
      <button class="btn btn-sm btn-accent"><i class="bi bi-check-lg me-1"></i>Salvar</button>
    </form>
  </div>
</div>
<?php endif; ?>

<!-- Licenças da empresa -->
<div class="card">
  <div class="card-body">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h6 class="fw-bold mb-0"><i class="bi bi-key me-2"></i>Licenças</h6>
      <button class="btn btn-sm btn-accent" data-bs-toggle="modal" data-bs-target="#modalGerar">
        <i class="bi bi-plus-lg me-1"></i>Gerar Licença
      </button>
    </div>
    <div class="table-responsive">
      <table class="table table-hover mb-0">
        <thead class="table-light">
          <tr><th>Chave</th><th>Tipo</th><th>Status</th><th>Dispositivo</th><th>Expira</th><th>Último acesso</th><th></th></tr>
        </thead>
        <tbody>
        <?php foreach ($licencas as $l): ?>
          <tr>
            <td><code><?= $l['chave'] ?></code></td>
            <td><?= ucfirst($l['tipo']) ?></td>
            <td><span class="badge badge-<?= $l['status'] ?>"><?= ucfirst($l['status']) ?></span></td>
            <td><?= $l['device_id'] ? '<i class="bi bi-phone-fill text-success"></i> ' . htmlspecialchars($l['device_nome'] ?? $l['device_id']) : '<span class="text-muted">—</span>' ?></td>
            <td><?= $l['expira_em'] ? date('d/m/Y', strtotime($l['expira_em'])) : '∞' ?></td>
            <td><?= $l['ultimo_acesso'] ? date('d/m/Y H:i', strtotime($l['ultimo_acesso'])) : '—' ?></td>
            <td><a href="<?= APP_URL ?>/admin/licencas/<?= $l['id'] ?>" class="btn btn-sm btn-outline-secondary">Ver</a></td>
          </tr>
        <?php endforeach; ?>
        <?php if (empty($licencas)): ?>
          <tr><td colspan="7" class="text-center text-muted py-3">Nenhuma licença.</td></tr>
        <?php endif; ?>
        </tbody>
      </table>
    </div>
  </div>
</div>

<!-- Modal Gerar Licença -->
<div class="modal fade" id="modalGerar" tabindex="-1">
  <div class="modal-dialog">
    <div class="modal-content">
      <form method="POST" action="<?= APP_URL ?>/admin/empresas/<?= $empresa['id'] ?>/gerar-licencas">
        <div class="modal-header">
          <h5 class="modal-title">Gerar Licenças — <?= htmlspecialchars($empresa['nome']) ?></h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
          <div class="row g-3">
            <div class="col-md-6">
              <label class="form-label">Tipo</label>
              <select name="tipo" id="tipoLicenca" class="form-select">
                <option value="trial">Trial</option>
                <option value="mensal">Mensal</option>
                <option value="anual">Anual</option>
                <option value="vitalicia">Vitalícia</option>
              </select>
            </div>
            <div class="col-md-6">
              <label class="form-label">Quantidade</label>
              <input type="number" name="quantidade" class="form-control" value="1" min="1" max="50">
            </div>
            <div id="wrapperDias" class="col-12">
              <label class="form-label">Dias de validade</label>
              <input type="number" name="dias" class="form-control" value="30" min="1">
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button>
          <button type="submit" class="btn btn-accent">Gerar</button>
        </div>
      </form>
    </div>
  </div>
</div>

<script>
document.getElementById('tipoLicenca').addEventListener('change', function () {
  document.getElementById('wrapperDias').style.display = this.value === 'vitalicia' ? 'none' : '';
});
</script>
