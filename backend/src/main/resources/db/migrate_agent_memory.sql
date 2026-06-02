-- ========================================
-- Agent 记忆管理层数据库迁移脚本
-- 创建核心记忆表和场景记忆表
-- ========================================

-- 1. 核心记忆表（长期记忆，永久保存）
CREATE TABLE IF NOT EXISTS `agent_core_memory` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `couple_id` BIGINT NOT NULL COMMENT '情侣ID',
    `memory_type` VARCHAR(50) NOT NULL COMMENT '记忆类型：BASIC_INFO(基本信息), PREFERENCE(偏好), CUSTOM(自定义)',
    `memory_key` VARCHAR(100) NOT NULL COMMENT '记忆键：如 love_date, birthday_him, dislike_food',
    `memory_value` TEXT NOT NULL COMMENT '记忆值',
    `importance` INT DEFAULT 3 COMMENT '重要程度 1-5',
    `source` VARCHAR(50) DEFAULT 'USER_INPUT' COMMENT '记忆来源：USER_INPUT, SYSTEM_SYNC, AI_EXTRACT',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_couple_type_key` (`couple_id`, `memory_type`, `memory_key`),
    KEY `idx_couple_id` (`couple_id`),
    KEY `idx_memory_type` (`memory_type`),
    KEY `idx_importance` (`importance`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 核心记忆表（长期记忆）';

-- 2. 场景记忆表（中期记忆，30天有效期）
CREATE TABLE IF NOT EXISTS `agent_scene_memory` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `couple_id` BIGINT NOT NULL COMMENT '情侣ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID（可为空，表示情侣共同记忆）',
    `memory_type` VARCHAR(50) NOT NULL COMMENT '记忆类型：DIALOGUE(对话记录), ACTION(操作记录), TEMP_PREFERENCE(临时偏好)',
    `content` TEXT NOT NULL COMMENT '记忆内容摘要',
    `raw_data` TEXT DEFAULT NULL COMMENT '原始数据（JSON格式）',
    `related_intent` VARCHAR(100) DEFAULT NULL COMMENT '关联的意图或操作类型',
    `importance` INT DEFAULT 3 COMMENT '重要程度 1-5',
    `expire_at` DATETIME DEFAULT NULL COMMENT '过期时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_couple_id` (`couple_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_memory_type` (`memory_type`),
    KEY `idx_expire_at` (`expire_at`),
    KEY `idx_related_intent` (`related_intent`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 场景记忆表（中期记忆）';

-- 3. 为 agent_memory 表添加缺失字段（如果表已存在）
-- 注意：如果 agent_memory 表不存在，请先创建
ALTER TABLE `agent_memory` 
ADD COLUMN IF NOT EXISTS `source` VARCHAR(50) DEFAULT 'USER_INPUT' COMMENT '记忆来源' AFTER `importance`;

-- ========================================
-- 示例数据（可选）
-- ========================================

-- 插入示例核心记忆
INSERT INTO `agent_core_memory` (`couple_id`, `memory_type`, `memory_key`, `memory_value`, `importance`, `source`) 
VALUES 
    (1, 'BASIC_INFO', 'love_date', '2024-01-01', 5, 'SYSTEM_SYNC'),
    (1, 'PREFERENCE', 'dislike_food', '不喜欢辣', 3, 'USER_INPUT'),
    (1, 'PREFERENCE', 'like_travel', '喜欢旅行', 4, 'USER_INPUT')
ON DUPLICATE KEY UPDATE `updated_at` = NOW();

-- 插入示例场景记忆
INSERT INTO `agent_scene_memory` (`couple_id`, `user_id`, `memory_type`, `content`, `related_intent`, `importance`, `expire_at`) 
VALUES 
    (1, 1, 'ACTION', '创建了约会备忘录：周末去看电影', 'CREATE_MEMO', 3, DATE_ADD(NOW(), INTERVAL 30 DAY)),
    (1, 1, 'DIALOGUE', '用户询问最近的纪念日', 'QUERY_ANNIVERSARY', 2, DATE_ADD(NOW(), INTERVAL 30 DAY))
ON DUPLICATE KEY UPDATE `updated_at` = NOW();

-- ========================================
-- 清理脚本（可选）
-- ========================================

-- 清理过期的场景记忆（可定期执行）
-- DELETE FROM `agent_scene_memory` WHERE `expire_at` IS NOT NULL AND `expire_at` <= NOW();

-- 清理超过30天的场景记忆
-- DELETE FROM `agent_scene_memory` WHERE `created_at` < DATE_SUB(NOW(), INTERVAL 30 DAY);
