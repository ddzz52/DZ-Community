USE couple;

CREATE TABLE IF NOT EXISTS t_wish_scratch_card (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  couple_id BIGINT NOT NULL,
  content VARCHAR(50) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0,
  reveal_mode TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  scratched_by BIGINT NULL,
  scratched_at DATETIME NULL,
  scratched_by_1 BIGINT NULL,
  scratched_at_1 DATETIME NULL,
  scratched_by_2 BIGINT NULL,
  scratched_at_2 DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_wish_scratch_couple_status_created ON t_wish_scratch_card(couple_id, status, created_at, id);
CREATE INDEX idx_wish_scratch_couple_scratched ON t_wish_scratch_card(couple_id, scratched_at, id);
CREATE INDEX idx_wish_scratch_couple_mode_status_created ON t_wish_scratch_card(couple_id, reveal_mode, status, created_at, id);
