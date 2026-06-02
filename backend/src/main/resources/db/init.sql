CREATE DATABASE IF NOT EXISTS couple DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE couple;

CREATE TABLE IF NOT EXISTS t_couple (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  signature VARCHAR(50) NULL,
  about_text TEXT NULL COMMENT '关于我们',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  couple_id BIGINT NOT NULL,
  username VARCHAR(32) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  nickname VARCHAR(32) NOT NULL,
  avatar_url VARCHAR(255) NULL,
  gender TINYINT NULL,
  love_date DATE NULL,
  zodiac VARCHAR(20) DEFAULT NULL COMMENT '星座',
  signature VARCHAR(50) NULL,
  temp_signature VARCHAR(50) NULL,
  signature_expire_time DATETIME NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_user_couple_id ON t_user(couple_id);

CREATE TABLE IF NOT EXISTS t_anniversary (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  couple_id BIGINT NOT NULL,
  title VARCHAR(64) NOT NULL,
  anniversary_date DATE NULL,
  calendar_type VARCHAR(10) NOT NULL DEFAULT 'SOLAR',
  lunar_month INT NULL,
  lunar_day INT NULL,
  lunar_leap TINYINT NOT NULL DEFAULT 0,
  type VARCHAR(20) NULL,
  icon VARCHAR(16) NULL,
  theme_color VARCHAR(16) NULL,
  cover_url VARCHAR(512) NULL,
  cover_thumb_url VARCHAR(512) NULL,
  note VARCHAR(200) NULL,
  reminder_enabled TINYINT NOT NULL DEFAULT 1,
  reminder_days_before INT NOT NULL DEFAULT 3,
  reminder_on_day TINYINT NOT NULL DEFAULT 1,
  pinned TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_anniversary_couple_date ON t_anniversary(couple_id, anniversary_date);
CREATE INDEX idx_anniversary_couple_pinned ON t_anniversary(couple_id, pinned);

CREATE TABLE IF NOT EXISTS t_diary (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  couple_id BIGINT NOT NULL,
  author_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  mood VARCHAR(16) NULL,
  private_flag TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_diary_couple_created ON t_diary(couple_id, created_at);

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

CREATE TABLE IF NOT EXISTS t_diary_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  diary_id BIGINT NOT NULL,
  couple_id BIGINT NOT NULL,
  url VARCHAR(512) NOT NULL,
  thumb_url VARCHAR(512) NULL,
  sort_no INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_diary_attachment_diary ON t_diary_attachment(diary_id, sort_no);

CREATE TABLE IF NOT EXISTS t_diary_like (
  diary_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  couple_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  PRIMARY KEY (diary_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_diary_like_couple ON t_diary_like(couple_id, created_at);

CREATE TABLE IF NOT EXISTS t_diary_favorite (
  diary_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  couple_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  PRIMARY KEY (diary_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_diary_favorite_couple ON t_diary_favorite(couple_id, created_at);

CREATE TABLE IF NOT EXISTS t_diary_pin (
  diary_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  couple_id BIGINT NOT NULL,
  pinned_at DATETIME NOT NULL,
  PRIMARY KEY (diary_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_diary_pin_couple ON t_diary_pin(couple_id, pinned_at);

CREATE TABLE IF NOT EXISTS t_diary_comment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  diary_id BIGINT NOT NULL,
  couple_id BIGINT NOT NULL,
  author_id BIGINT NOT NULL,
  content VARCHAR(100) NOT NULL,
  created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_diary_comment_diary ON t_diary_comment(diary_id, created_at);

CREATE TABLE IF NOT EXISTS t_photo (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  couple_id BIGINT NOT NULL,
  uploader_id BIGINT NOT NULL,
  album_id BIGINT NULL,
  url VARCHAR(512) NOT NULL,
  thumb_url VARCHAR(512) NULL,
  title VARCHAR(64) NULL,
  location VARCHAR(64) NULL,
  mood VARCHAR(16) NULL,
  tags VARCHAR(255) NULL,
  cover_flag TINYINT NOT NULL DEFAULT 0,
  cover_set_at DATETIME NULL,
  shot_at DATETIME NULL,
  deleted_flag TINYINT NOT NULL DEFAULT 0,
  deleted_at DATETIME NULL,
  deleted_from_album_id BIGINT NULL,
  created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_photo_couple_created ON t_photo(couple_id, created_at);
CREATE INDEX idx_photo_couple_cover ON t_photo(couple_id, cover_flag, cover_set_at);

CREATE TABLE IF NOT EXISTS t_photo_like (
  photo_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  couple_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  PRIMARY KEY (photo_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_photo_like_couple ON t_photo_like(couple_id, created_at);

CREATE TABLE IF NOT EXISTS t_photo_comment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  photo_id BIGINT NOT NULL,
  couple_id BIGINT NOT NULL,
  author_id BIGINT NOT NULL,
  content VARCHAR(100) NOT NULL,
  created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_photo_comment_photo ON t_photo_comment(photo_id, created_at);

CREATE TABLE IF NOT EXISTS t_album (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  couple_id BIGINT NOT NULL,
  name VARCHAR(20) NOT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  deletable TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_album_couple_sort ON t_album(couple_id, sort_no, id);
CREATE UNIQUE INDEX uk_album_couple_name ON t_album(couple_id, name);
CREATE INDEX idx_photo_album ON t_photo(album_id, created_at);

CREATE TABLE IF NOT EXISTS t_user_settings (
  user_id BIGINT PRIMARY KEY,
  couple_id BIGINT NOT NULL,
  message_enabled TINYINT NOT NULL DEFAULT 1,
  reminder_enabled TINYINT NOT NULL DEFAULT 1,
  reminder_window_start VARCHAR(5) NULL,
  reminder_window_end VARCHAR(5) NULL,
  dnd_enabled TINYINT NOT NULL DEFAULT 0,
  dnd_start VARCHAR(5) NULL,
  dnd_end VARCHAR(5) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_user_settings_couple ON t_user_settings(couple_id);

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

CREATE TABLE IF NOT EXISTS t_notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  couple_id BIGINT NOT NULL,
  type VARCHAR(32) NOT NULL,
  title VARCHAR(64) NOT NULL,
  content VARCHAR(255) NULL,
  ref_id BIGINT NULL,
  ref_date DATE NULL,
  read_flag TINYINT NOT NULL DEFAULT 0,
  silent_flag TINYINT NOT NULL DEFAULT 0,
  archived_flag TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_notification_user_read ON t_notification(user_id, read_flag, created_at);
CREATE INDEX idx_notification_user_archived ON t_notification(user_id, archived_flag, created_at);
CREATE INDEX idx_notification_couple_created ON t_notification(couple_id, created_at);
CREATE UNIQUE INDEX uk_notification_dedupe ON t_notification(user_id, type, ref_id, ref_date);

CREATE TABLE IF NOT EXISTS t_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  couple_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  msg_type VARCHAR(16) NOT NULL,
  content VARCHAR(500) NOT NULL,
  delivered_at DATETIME NULL,
  read_at DATETIME NULL,
  recalled_flag TINYINT NOT NULL DEFAULT 0,
  recalled_by BIGINT NULL,
  recalled_at DATETIME NULL,
  created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_message_couple_id ON t_message(couple_id, id);
CREATE INDEX idx_message_couple_created ON t_message(couple_id, created_at);
CREATE INDEX idx_message_unread ON t_message(couple_id, read_at, sender_id, id);

CREATE TABLE IF NOT EXISTS t_sticker (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  couple_id BIGINT NOT NULL,
  url VARCHAR(512) NOT NULL,
  created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_sticker_user ON t_sticker(user_id, created_at);
CREATE INDEX idx_sticker_couple ON t_sticker(couple_id, created_at);

CREATE TABLE IF NOT EXISTS t_message_cursor (
  user_id BIGINT NOT NULL,
  couple_id BIGINT NOT NULL,
  device_id VARCHAR(64) NOT NULL,
  last_read_id BIGINT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (user_id, device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_message_cursor_couple ON t_message_cursor(couple_id, updated_at);

CREATE TABLE IF NOT EXISTS t_password_reset_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  couple_id BIGINT NOT NULL,
  code_hash VARCHAR(100) NOT NULL,
  expires_at DATETIME NOT NULL,
  used_flag TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_pwd_reset_user_created ON t_password_reset_request(user_id, created_at);

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
