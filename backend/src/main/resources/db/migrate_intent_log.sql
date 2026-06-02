-- ============================================================
-- 意图识别日志表
-- 用于记录每次意图识别的结果，支持后续分析和模型调优
-- ============================================================

CREATE TABLE IF NOT EXISTS `agent_intent_log` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `couple_id`         BIGINT UNSIGNED NOT NULL COMMENT '情侣ID',
    `user_id`           BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `message`           VARCHAR(500) NOT NULL DEFAULT '' COMMENT '用户原始消息',
    `recognized_intent` VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '识别出的意图编码',
    `params_json`       VARCHAR(1000) DEFAULT NULL COMMENT '提取的参数JSON',
    `confidence`        DECIMAL(3,2) DEFAULT 0.00 COMMENT '置信度 0.00~1.00',
    `source`            VARCHAR(16)  NOT NULL DEFAULT 'RULE' COMMENT '识别来源: LLM/RULE/CACHE',
    `latency_ms`        BIGINT UNSIGNED DEFAULT 0 COMMENT '识别耗时(毫秒)',
    `created_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (`id`),
    INDEX `idx_couple_id` (`couple_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_intent_source` (`recognized_intent`, `source`),
    INDEX `idx_couple_created` (`couple_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='AI管家意图识别日志';

-- 定期清理90天前的日志（可选定时任务）
-- DELETE FROM agent_intent_log WHERE created_at < DATE_SUB(NOW(), INTERVAL 90 DAY);
