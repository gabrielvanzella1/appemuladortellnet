-- ============================================================
-- Migração: versões do app Android, com alvo por empresa
-- Rode uma única vez no banco já existente (local ou Hostinger).
-- ============================================================

CREATE TABLE IF NOT EXISTS app_versoes (
  id                    INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  version_code          INT UNSIGNED NOT NULL,
  version_name          VARCHAR(30) NOT NULL,
  changelog             TEXT NOT NULL,
  empresa_exclusiva_id  INT UNSIGNED NULL,       -- NULL = versão geral, serve pra qualquer empresa
  arquivo_apk           VARCHAR(255) NOT NULL,   -- nome do arquivo em public/downloads/apks/
  criado_por            INT UNSIGNED NULL,
  criada_em             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uniq_version_code (version_code),
  FOREIGN KEY (empresa_exclusiva_id) REFERENCES empresas(id) ON DELETE SET NULL,
  FOREIGN KEY (criado_por) REFERENCES usuarios(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE empresas
  ADD COLUMN app_versao_alvo_id INT UNSIGNED NULL AFTER lock_conexao_senha,
  ADD FOREIGN KEY (app_versao_alvo_id) REFERENCES app_versoes(id) ON DELETE SET NULL;
