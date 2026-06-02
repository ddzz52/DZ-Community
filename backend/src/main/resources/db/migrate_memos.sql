CREATE TABLE IF NOT EXISTS t_memo_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  couple_id BIGINT NOT NULL,
  name VARCHAR(20) NOT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  system_flag TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE UNIQUE INDEX uk_memo_category_couple_name ON t_memo_category(couple_id, name);
CREATE INDEX idx_memo_category_couple_sort ON t_memo_category(couple_id, sort_no, id);

CREATE TABLE IF NOT EXISTS t_memo (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  couple_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  title VARCHAR(30) NOT NULL,
  content VARCHAR(500) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT NOT NULL,
  updated_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_memo_couple_status_updated ON t_memo(couple_id, status, updated_at, id);
CREATE INDEX idx_memo_couple_category_status_updated ON t_memo(couple_id, category_id, status, updated_at, id);
