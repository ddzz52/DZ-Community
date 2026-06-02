USE couple;

CREATE TABLE IF NOT EXISTS t_wish_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  couple_id BIGINT NOT NULL,
  content VARCHAR(100) NOT NULL,
  expected_at DATE NOT NULL,
  priority TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(200) NOT NULL DEFAULT '',
  status TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_by BIGINT NOT NULL,
  updated_at DATETIME NOT NULL,
  completed_at DATETIME NULL,
  canceled_at DATETIME NULL,
  source_type VARCHAR(32) NULL,
  source_id BIGINT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_wish_item_couple_status_priority_expected ON t_wish_item(couple_id, status, priority, expected_at, id);
CREATE INDEX idx_wish_item_couple_expected ON t_wish_item(couple_id, expected_at, id);
CREATE INDEX idx_wish_item_source ON t_wish_item(source_type, source_id);
