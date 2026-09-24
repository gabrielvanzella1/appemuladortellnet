<?php if ($flash): ?>
<div class="alert alert-<?= $flash['type'] === 'success' ? 'success' : 'danger' ?> alert-dismissible fade show">
  <?= $flash['message'] ?><button type="button" class="btn-close" data-bs-dismiss="alert"></button>
</div>
<?php endif; ?>

<h4 class="fw-bold mb-4"><i class="bi bi-shield-lock me-2"></i>Alterar senha</h4>

<div class="card" style="max-width:480px">
  <div class="card-body">
    <?php if ($erro): ?>
      <div class="alert alert-danger py-2"><?= htmlspecialchars($erro) ?></div>
    <?php endif; ?>
    <form method="post" action="<?= APP_URL ?>/conta/senha" autocomplete="off">
      <?php foreach ([
          'senha_atual'       => ['Senha atual', 'current-password'],
          'senha_nova'        => ['Nova senha (mínimo 8 caracteres)', 'new-password'],
          'senha_confirmacao' => ['Confirme a nova senha', 'new-password'],
      ] as $campo => [$label, $ac]): ?>
      <div class="mb-3">
        <label class="form-label" for="<?= $campo ?>"><?= $label ?></label>
        <div class="input-group">
          <input type="password" name="<?= $campo ?>" id="<?= $campo ?>" class="form-control" required
                 <?= $campo !== 'senha_atual' ? 'minlength="8"' : '' ?> autocomplete="<?= $ac ?>">
          <button type="button" class="btn btn-outline-secondary" aria-label="Mostrar senha"
                  onclick="const s=document.getElementById('<?= $campo ?>'), v=s.type==='password';
                           s.type=v?'text':'password';
                           this.firstElementChild.className=v?'bi bi-eye-slash':'bi bi-eye';">
            <i class="bi bi-eye"></i>
          </button>
        </div>
      </div>
      <?php endforeach; ?>
      <button type="submit" class="btn btn-accent w-100"><i class="bi bi-check-lg me-1"></i> Salvar nova senha</button>
    </form>
  </div>
</div>
