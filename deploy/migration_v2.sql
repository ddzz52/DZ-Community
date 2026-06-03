-- ==================== DZ-Community V2 迁移脚本 ====================
-- 运行方式: mysql -u root -p couple < migration_v2.sql

USE couple;

-- 1. t_user 增加角色字段（MySQL 8.0 需用存储过程判断列是否存在）
DROP PROCEDURE IF EXISTS add_role_column;
DELIMITER $$
CREATE PROCEDURE add_role_column()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'couple' AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'role'
    ) THEN
        ALTER TABLE t_user ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'USER' COMMENT '角色: ADMIN/USER' AFTER gender;
    END IF;
END$$
DELIMITER ;
CALL add_role_column();
DROP PROCEDURE IF EXISTS add_role_column;

-- 2. 第一个注册的用户设为管理员
UPDATE t_user SET role = 'ADMIN' WHERE id = (SELECT MIN(id) FROM (SELECT MIN(id) AS id FROM t_user) AS t);

-- 3. 心愿刮刮卡表
CREATE TABLE IF NOT EXISTS t_wish_scratch_card (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  couple_id BIGINT NOT NULL,
  content VARCHAR(50) NOT NULL COMMENT '心愿内容',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0=未刮 1=已刮',
  reveal_mode TINYINT NOT NULL DEFAULT 0 COMMENT '0=即刮即看 1=双人都刮才显示',
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  scratched_by BIGINT NULL,
  scratched_at DATETIME NULL,
  scratched_by_1 BIGINT NULL,
  scratched_at_1 DATETIME NULL,
  scratched_by_2 BIGINT NULL,
  scratched_at_2 DATETIME NULL,
  INDEX idx_wish_scratch_couple_status (couple_id, status),
  INDEX idx_wish_scratch_couple_created (couple_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 心愿清单表
CREATE TABLE IF NOT EXISTS t_wish_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  couple_id BIGINT NOT NULL,
  content VARCHAR(200) NOT NULL COMMENT '心愿内容',
  expected_at VARCHAR(50) NULL,
  priority INT NOT NULL DEFAULT 0 COMMENT '0=普通 1=重要 2=紧急',
  remark VARCHAR(500) NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0=待完成 1=已完成 2=已取消',
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_by BIGINT NULL,
  updated_at DATETIME NULL,
  completed_at DATETIME NULL,
  canceled_at DATETIME NULL,
  source_type VARCHAR(20) NULL,
  source_id BIGINT NULL,
  INDEX idx_wish_item_couple_status (couple_id, status),
  INDEX idx_wish_item_couple_priority (couple_id, priority, expected_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. AI管家 - 交互记录表
CREATE TABLE IF NOT EXISTS agent_interaction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  couple_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  interaction_type VARCHAR(20) NULL,
  intent VARCHAR(50) NULL,
  user_message TEXT NULL,
  agent_response TEXT NULL,
  tools_used VARCHAR(500) NULL,
  emotion_score DOUBLE NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_agent_interaction_couple (couple_id, created_at),
  INDEX idx_agent_interaction_user (user_id, created_at),
  INDEX idx_agent_interaction_type (couple_id, interaction_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. AI管家 - 意图识别日志表
CREATE TABLE IF NOT EXISTS agent_intent_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  couple_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  message VARCHAR(500) NULL,
  recognized_intent VARCHAR(50) NULL,
  params_json TEXT NULL,
  confidence DOUBLE NULL,
  source VARCHAR(20) NULL,
  latency_ms BIGINT NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_agent_intent_log_couple (couple_id, created_at),
  INDEX idx_agent_intent_log_source (source, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. AI管家 - 核心记忆表（长期）
CREATE TABLE IF NOT EXISTS agent_core_memory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  couple_id BIGINT NOT NULL,
  memory_type VARCHAR(30) NOT NULL,
  memory_key VARCHAR(100) NOT NULL,
  memory_value TEXT NULL,
  importance INT NOT NULL DEFAULT 3,
  source VARCHAR(30) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  INDEX idx_agent_core_mem_couple (couple_id, memory_type),
  UNIQUE INDEX uk_agent_core_mem_key (couple_id, memory_type, memory_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. AI管家 - 场景记忆表（中期，30天有效）
CREATE TABLE IF NOT EXISTS agent_scene_memory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  couple_id BIGINT NOT NULL,
  user_id BIGINT NULL,
  memory_type VARCHAR(30) NOT NULL,
  content TEXT NULL,
  raw_data TEXT NULL,
  related_intent VARCHAR(50) NULL,
  importance INT NOT NULL DEFAULT 1,
  expire_at DATETIME NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_agent_scene_mem_couple (couple_id, memory_type, expire_at),
  INDEX idx_agent_scene_mem_expire (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. AI管家 - 通用记忆表（支持过期）
CREATE TABLE IF NOT EXISTS agent_memory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  couple_id BIGINT NOT NULL,
  memory_type VARCHAR(30) NOT NULL,
  memory_key VARCHAR(100) NOT NULL,
  memory_value TEXT NULL,
  importance INT NOT NULL DEFAULT 3,
  expire_at DATETIME NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  INDEX idx_agent_mem_couple (couple_id, memory_type),
  UNIQUE INDEX uk_agent_mem_key (couple_id, memory_type, memory_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. 系统公告表
CREATE TABLE IF NOT EXISTS t_system_announcement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  content VARCHAR(500) NOT NULL COMMENT '公告内容',
  active TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  INDEX idx_announcement_active (active, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
