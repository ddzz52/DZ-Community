CREATE TABLE IF NOT EXISTS t_period_settings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  couple_id BIGINT NOT NULL,
  owner_user_id BIGINT NOT NULL,
  enc_data TEXT NOT NULL,
  created_by BIGINT NOT NULL,
  updated_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE UNIQUE INDEX uk_period_settings_couple ON t_period_settings(couple_id);
CREATE INDEX idx_period_settings_owner ON t_period_settings(owner_user_id, updated_at);

