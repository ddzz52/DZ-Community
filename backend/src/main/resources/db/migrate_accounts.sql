CREATE TABLE IF NOT EXISTS t_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  couple_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  category VARCHAR(32) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  occurred_at DATETIME NOT NULL,
  remark VARCHAR(200) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_account_couple_occurred ON t_account(couple_id, occurred_at, id);
CREATE INDEX idx_account_couple_user_occurred ON t_account(couple_id, user_id, occurred_at);
CREATE INDEX idx_account_couple_category_occurred ON t_account(couple_id, category, occurred_at);

