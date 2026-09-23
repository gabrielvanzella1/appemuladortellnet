-- ============================================================
-- Migração: Cerca digital por Wi-Fi (rastreamento de coletores sem GPS)
-- A cerca é a rede Wi-Fi do CD: cada coletor informa o roteador (AP/BSSID)
-- em que está conectado; a empresa marca quais roteadores fazem parte da
-- cerca e dá nome às zonas. Horários em UTC, como o resto do banco.
-- Rode uma única vez no banco já existente (local ou Hostinger).
-- ============================================================

-- Liga/desliga e parâmetros por empresa
ALTER TABLE empresas
  ADD COLUMN rastreamento_ativo         TINYINT(1)        NOT NULL DEFAULT 0          AFTER ativo,
  ADD COLUMN limite_sem_comunicacao_min SMALLINT UNSIGNED NOT NULL DEFAULT 15         AFTER rastreamento_ativo,
  ADD COLUMN limite_bateria_pct         TINYINT UNSIGNED  NOT NULL DEFAULT 15         AFTER limite_sem_comunicacao_min,
  ADD COLUMN expediente_inicio          TIME              NOT NULL DEFAULT '06:00:00' AFTER limite_bateria_pct,
  ADD COLUMN expediente_fim             TIME              NOT NULL DEFAULT '22:00:00' AFTER expediente_inicio,
  ADD COLUMN expediente_dias            VARCHAR(20)       NOT NULL DEFAULT '1,2,3,4,5,6' AFTER expediente_fim; -- ISO: 1=seg ... 7=dom

-- Estado atual do coletor (a chave tem 25 chars: SCTE-XXXXXX-XXXXXX-XXXXXX; a 24 cortava)
ALTER TABLE dispositivos
  MODIFY COLUMN chave_licenca VARCHAR(32) NULL,
  ADD COLUMN ultimo_sinal_em    DATETIME          NULL,
  ADD COLUMN ultimo_bssid       VARCHAR(17)       NULL,
  ADD COLUMN ultimo_ssid        VARCHAR(64)       NULL,
  ADD COLUMN ultimo_rssi        SMALLINT          NULL,
  ADD COLUMN ultima_bateria_pct TINYINT UNSIGNED  NULL,
  ADD COLUMN ultimo_carregando  TINYINT(1)        NULL,
  ADD COLUMN ultima_lat         DECIMAL(9,6)      NULL,
  ADD COLUMN ultima_lng         DECIMAL(9,6)      NULL;

-- Roteadores descobertos pelos coletores (a empresa marca os da cerca e nomeia a zona)
CREATE TABLE IF NOT EXISTS pontos_acesso (
  id             INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  empresa_id     INT UNSIGNED NOT NULL,
  bssid          VARCHAR(17)  NOT NULL,             -- aa:bb:cc:dd:ee:ff (minúsculo)
  ssid           VARCHAR(64)  NULL,
  zona           VARCHAR(100) NULL,                 -- "Doca 1", "Expedição"...
  na_cerca       TINYINT(1)   NOT NULL DEFAULT 0,
  primeiro_visto DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ultimo_visto   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_empresa_bssid (empresa_id, bssid),
  FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Histórico de sinais (90 dias; limpo em lotes no próprio envio, sem cron).
-- lat/lng/precisão já ficam prontos para coletores com GPS.
CREATE TABLE IF NOT EXISTS dispositivo_sinais (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  device_id    VARCHAR(100)      NOT NULL,
  empresa_id   INT UNSIGNED      NOT NULL,
  bssid        VARCHAR(17)       NULL,
  ssid         VARCHAR(64)       NULL,
  rssi         SMALLINT          NULL,
  lat          DECIMAL(9,6)      NULL,
  lng          DECIMAL(9,6)      NULL,
  precisao_m   SMALLINT UNSIGNED NULL,
  bateria_pct  TINYINT UNSIGNED  NULL,
  carregando   TINYINT(1)        NULL,
  ip           VARCHAR(45)       NULL,
  capturado_em DATETIME          NOT NULL,          -- quando o coletor leu
  recebido_em  DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_device_data  (device_id, capturado_em),
  INDEX idx_empresa_data (empresa_id, capturado_em)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Alertas com início/fim (fim_em NULL = ainda aberto)
CREATE TABLE IF NOT EXISTS dispositivo_alertas (
  id          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  empresa_id  INT UNSIGNED NOT NULL,
  device_id   VARCHAR(100) NOT NULL,
  device_nome VARCHAR(200) NULL,
  tipo        ENUM('fora_da_cerca','sem_comunicacao','bateria_baixa') NOT NULL,
  inicio_em   DATETIME     NOT NULL,
  fim_em      DATETIME     NULL,
  bssid       VARCHAR(17)  NULL,                    -- roteador de referência (rede estranha / último visto)
  detalhe     VARCHAR(200) NULL,
  INDEX idx_empresa_aberto (empresa_id, fim_em),
  INDEX idx_device_tipo    (device_id, tipo, fim_em),
  FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
