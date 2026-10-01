<?php if ($flash): ?>
<div class="alert alert-<?= $flash['type'] === 'success' ? 'success' : 'warning' ?> alert-dismissible fade show">
  <?= $flash['message'] ?><button type="button" class="btn-close" data-bs-dismiss="alert"></button>
</div>
<?php endif; ?>

<div class="d-flex align-items-center mb-4 gap-2">
  <h4 class="fw-bold mb-0"><i class="bi bi-sliders me-2"></i>Configurações</h4>
</div>

<form method="POST" action="<?= APP_URL ?>/admin/configuracoes/salvar">

  <!-- Notificação de novas solicitações -->
  <div class="card mb-3">
    <div class="card-header fw-semibold"><i class="bi bi-envelope me-2"></i>Notificação de novas solicitações</div>
    <div class="card-body">
      <p class="text-muted small mb-3">
        E-mail que recebe aviso quando chega uma nova solicitação de licença em
        <a href="<?= APP_URL ?>/admin/licencas">Licenças</a>.
      </p>
      <label class="form-label fw-semibold">E-mail para receber os avisos</label>
      <input type="email" name="email_notificacoes" class="form-control" style="max-width:320px"
             value="<?= htmlspecialchars($cfg['email_notificacoes'] ?? '') ?>" placeholder="contato@scante.com.br">
    </div>
  </div>

  <div class="d-flex gap-2">
    <button type="submit" class="btn btn-accent px-4">
      <i class="bi bi-floppy me-1"></i>Salvar configurações
    </button>
    <a href="<?= APP_URL ?>/admin" class="btn btn-outline-secondary">Cancelar</a>
  </div>

</form>
