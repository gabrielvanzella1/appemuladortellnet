<div class="card p-4">
  <h5 class="mb-4 fw-bold text-center">Entrar</h5>

  <?php if ($erro): ?>
  <div class="alert alert-danger py-2"><?= $erro ?></div>
  <?php endif; ?>

  <form method="POST" action="<?= APP_URL ?>/login">
    <div class="mb-3">
      <label class="form-label">E-mail</label>
      <input type="email" name="email" class="form-control" required autofocus>
    </div>
    <div class="mb-4">
      <label class="form-label" for="senha">Senha</label>
      <div class="input-group">
        <input type="password" name="senha" id="senha" class="form-control" required>
        <button type="button" class="btn btn-outline-secondary" aria-label="Mostrar senha"
                onclick="const s=document.getElementById('senha'), v=s.type==='password';
                         s.type=v?'text':'password';
                         this.firstElementChild.className=v?'bi bi-eye-slash':'bi bi-eye';
                         this.setAttribute('aria-label', v?'Ocultar senha':'Mostrar senha');">
          <i class="bi bi-eye"></i>
        </button>
      </div>
    </div>
    <button type="submit" class="btn btn-primary w-100">Entrar</button>
  </form>

  <div class="text-center mt-4 pt-3 border-top">
    <small class="text-muted">
      O acesso é liberado automaticamente conforme sua conta:<br>
      <strong>Painel Geral</strong> (gestão de todas as empresas) ou <strong>Painel Empresa</strong> (sua empresa).
    </small>
  </div>
</div>
