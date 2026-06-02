ALTER TABLE t_user ADD COLUMN signature VARCHAR(50) NULL AFTER love_date;
ALTER TABLE t_user ADD COLUMN temp_signature VARCHAR(50) NULL AFTER signature;
ALTER TABLE t_user ADD COLUMN signature_expire_time DATETIME NULL AFTER temp_signature;
