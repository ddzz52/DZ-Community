USE couple;

ALTER TABLE t_wish_scratch_card
  ADD COLUMN reveal_mode TINYINT NOT NULL DEFAULT 0 AFTER status,
  ADD COLUMN scratched_by_1 BIGINT NULL AFTER scratched_at,
  ADD COLUMN scratched_at_1 DATETIME NULL AFTER scratched_by_1,
  ADD COLUMN scratched_by_2 BIGINT NULL AFTER scratched_at_1,
  ADD COLUMN scratched_at_2 DATETIME NULL AFTER scratched_by_2;

CREATE INDEX idx_wish_scratch_couple_mode_status_created ON t_wish_scratch_card(couple_id, reveal_mode, status, created_at, id);
