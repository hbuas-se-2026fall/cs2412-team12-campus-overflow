-- ============================================================================
-- CampusOverflow 初始 schema（Lab 4 · Step 3）
-- 团队：第 12 组（PO 倪昊宇 / SM 陈玉豪、刘铭洋 / Dev 陶浩峰、程靖轩）
--
-- 约定：
--   1) 表名单数 snake_case（Lab 3 统一语言 · 命名规约）
--   2) 跨上下文只存 ID、不建外键（question.asker_id → user.id 由应用层保证；
--      身份上下文将来可能独立部署，数据库级外键会把两个上下文焊死）
--   3) 同上下文内保留外键（answer.question_id → question.id）
--   4) 所有时间列存 UTC，展示时由前端按本地时区格式化
-- ============================================================================

CREATE TABLE `user`
(
    `id`            VARCHAR(36)  NOT NULL COMMENT '账号标识（UUID）',
    `email`         VARCHAR(254) NOT NULL COMMENT '邮箱，入库前统一小写，唯一索引即 INV-U-01',
    `nickname`      VARCHAR(32)  NOT NULL COMMENT '昵称',
    `password_hash` VARCHAR(100) NOT NULL COMMENT 'bcrypt 哈希，绝不存明文（INV-U-03）',
    `status`        VARCHAR(16)  NOT NULL COMMENT 'pending / active / disabled（INV-U-04 单向流转）',
    `created_at`    DATETIME(6)  NOT NULL COMMENT '创建时间（UTC）',
    `updated_at`    DATETIME(6)  NOT NULL COMMENT '最后更新时间（UTC）',
    `version`       BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_email` (`email`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='账号（身份与访问上下文）';

CREATE TABLE `question`
(
    `id`                 VARCHAR(36)  NOT NULL COMMENT '问题标识（UUID）',
    `asker_id`           VARCHAR(36)  NOT NULL COMMENT '提问者标识（跨上下文弱引用，不建外键）',
    `title`              VARCHAR(150) NOT NULL COMMENT '标题 5–150 字（INV-Q-01）',
    `body`               MEDIUMTEXT   NOT NULL COMMENT 'Markdown 原文，≤20000 字（INV-Q-01）',
    `tags`               VARCHAR(512) NOT NULL DEFAULT '' COMMENT '逗号分隔标签，≤5 个且在 SK-01 词表内（INV-Q-02）',
    `accepted_answer_id` VARCHAR(36)           DEFAULT NULL COMMENT '被采纳答案，全问题唯一（INV-Q-04）',
    `status`             VARCHAR(16)  NOT NULL COMMENT 'open / solved / hidden（INV-Q-05）',
    `vote_count`         INT          NOT NULL DEFAULT 0 COMMENT '票数投影值，可正可负',
    `created_at`         DATETIME(6)  NOT NULL COMMENT '创建时间（UTC）',
    `updated_at`         DATETIME(6)  NOT NULL COMMENT '最后更新时间（UTC）',
    `version`            BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_question_created_at` (`created_at`),
    KEY `idx_question_asker_id` (`asker_id`),
    KEY `idx_question_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='问题（问答核心上下文，AGG-02）';

CREATE TABLE `answer`
(
    `id`          VARCHAR(36)  NOT NULL COMMENT '回答标识（UUID）',
    `question_id` VARCHAR(36)  NOT NULL COMMENT '所属问题（同上下文，保留外键）',
    `author_id`   VARCHAR(36)  NOT NULL COMMENT '回答者标识（跨上下文弱引用，不建外键）',
    `body`        MEDIUMTEXT   NOT NULL COMMENT 'Markdown 原文，≤10000 字（INV-A-01）',
    `status`      VARCHAR(16)  NOT NULL COMMENT 'normal / hidden',
    `accepted`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否被采纳（INV-A-04：被采纳不可删除）',
    `created_at`  DATETIME(6)  NOT NULL COMMENT '创建时间（UTC）',
    `updated_at`  DATETIME(6)  NOT NULL COMMENT '最后更新时间（UTC）',
    `version`     BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_answer_question_id` (`question_id`, `created_at`),
    KEY `idx_answer_author_id` (`author_id`),
    CONSTRAINT `fk_answer_question` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='回答（问答核心上下文，AGG-03）';
