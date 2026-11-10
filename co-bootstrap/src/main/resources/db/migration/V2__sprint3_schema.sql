-- ============================================================================
-- CampusOverflow Sprint 3 schema（Lab 6）
-- 投票、评论、标签词表与派生索引、声誉账户与明细
--
-- 设计要点（详见 docs/adr/ADR-003-sprint3-core-features.md）：
--   1) vote 用 (target_type, target_id, voter_id) 唯一键保证"一人一票"（INV-V-01）
--   2) question_tag 是 question.tags 的派生索引，只为"按标签筛选"服务，
--      写入时由仓储同步；历史数据用递归 CTE 一次性回填
--   3) reputation_entry.event_id 唯一键保证事件重复投递时幂等（INV-R-02）
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 投票（问答核心，AGG-04）
-- ---------------------------------------------------------------------------
CREATE TABLE `vote`
(
    `id`          VARCHAR(36) NOT NULL COMMENT '投票记录标识（UUID）',
    `target_type` VARCHAR(16) NOT NULL COMMENT 'question / answer',
    `target_id`   VARCHAR(36) NOT NULL COMMENT '目标标识',
    `voter_id`    VARCHAR(36) NOT NULL COMMENT '投票人（跨上下文弱引用）',
    `vote_value`  VARCHAR(8)  NOT NULL COMMENT 'up / down（列名避开 SQL 保留字 value）',
    `created_at`  DATETIME(6) NOT NULL,
    `updated_at`  DATETIME(6) NOT NULL,
    `version`     BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_vote_target_voter` (`target_type`, `target_id`, `voter_id`),
    KEY `idx_vote_voter` (`voter_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='投票（AGG-04，一人一目标一票）';

-- 回答也要有票数投影（US-05 按票数排序、US-07 投票）
ALTER TABLE `answer`
    ADD COLUMN `vote_count` INT NOT NULL DEFAULT 0 COMMENT '票数投影值（UPDATE 前缓存）' AFTER `accepted`;

-- ---------------------------------------------------------------------------
-- 评论（问答核心，AGG-05）
-- ---------------------------------------------------------------------------
CREATE TABLE `comment`
(
    `id`          VARCHAR(36)  NOT NULL COMMENT '评论标识（UUID）',
    `target_type` VARCHAR(16)  NOT NULL COMMENT 'question / answer',
    `target_id`   VARCHAR(36)  NOT NULL COMMENT '被评论内容标识',
    `author_id`   VARCHAR(36)  NOT NULL COMMENT '评论者（跨上下文弱引用）',
    `body`        VARCHAR(500) NOT NULL COMMENT '评论内容，≤500 字（INV-C-01）',
    `created_at`  DATETIME(6)  NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_comment_target` (`target_type`, `target_id`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='评论（AGG-05，不盖楼）';

-- ---------------------------------------------------------------------------
-- 标签词表 SK-01（US-10：从只读白名单改为可扩展词表）
-- ---------------------------------------------------------------------------
CREATE TABLE `tag`
(
    `code`       VARCHAR(20) NOT NULL COMMENT '归一化后的标签（小写）',
    `label`      VARCHAR(20) NOT NULL COMMENT '展示用标签',
    `created_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='标签词表（SK-01）';

INSERT INTO `tag` (`code`, `label`, `created_at`)
VALUES ('java', 'java', UTC_TIMESTAMP(6)),
       ('spring-boot', 'spring-boot', UTC_TIMESTAMP(6)),
       ('spring-data-jpa', 'spring-data-jpa', UTC_TIMESTAMP(6)),
       ('mysql', 'mysql', UTC_TIMESTAMP(6)),
       ('flyway', 'flyway', UTC_TIMESTAMP(6)),
       ('ddd', 'ddd', UTC_TIMESTAMP(6)),
       ('maven', 'maven', UTC_TIMESTAMP(6)),
       ('junit', 'junit', UTC_TIMESTAMP(6)),
       ('docker', 'docker', UTC_TIMESTAMP(6)),
       ('git', 'git', UTC_TIMESTAMP(6)),
       ('react', 'react', UTC_TIMESTAMP(6)),
       ('typescript', 'typescript', UTC_TIMESTAMP(6)),
       ('数据库', '数据库', UTC_TIMESTAMP(6)),
       ('算法', '算法', UTC_TIMESTAMP(6)),
       ('校园生活', '校园生活', UTC_TIMESTAMP(6)),
       ('选课', '选课', UTC_TIMESTAMP(6)),
       ('考研', '考研', UTC_TIMESTAMP(6)),
       ('实习', '实习', UTC_TIMESTAMP(6)),
       ('保研', '保研', UTC_TIMESTAMP(6)),
       ('实验室', '实验室', UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE `label` = VALUES(`label`);

-- 派生索引：question.tags 变更时由仓储同步（见 QuestionRepositoryAdapter）
CREATE TABLE `question_tag`
(
    `question_id` VARCHAR(36) NOT NULL,
    `tag`         VARCHAR(20) NOT NULL,
    PRIMARY KEY (`question_id`, `tag`),
    KEY `idx_question_tag_tag` (`tag`, `question_id`),
    CONSTRAINT `fk_question_tag_question` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='问题-标签派生索引（读模型，可重建）';

-- 历史数据回填：把 question.tags（逗号分隔）拆成行。
-- 目标表此时必然为空（V2 刚建表），且每个问题的标签在 TagSet 里已去重，
-- 因此不需要 ON DUPLICATE KEY UPDATE（写了反而会因 tag 列名歧义报 1052）。
INSERT INTO `question_tag` (`question_id`, `tag`)
WITH RECURSIVE `seq` AS (SELECT 1 AS n UNION ALL SELECT n + 1 FROM `seq` WHERE n < 10),
               `split` AS (SELECT q.`id` AS question_id,
                                  TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(q.`tags`, ',', s.n), ',', -1)) AS tag
                           FROM `question` q
                                    JOIN `seq` s
                                         ON s.n <= 1 + LENGTH(q.`tags`) - LENGTH(REPLACE(q.`tags`, ',', ''))
                           WHERE q.`tags` <> '')
SELECT `question_id`, `tag`
FROM `split`
WHERE `tag` <> '';

-- ---------------------------------------------------------------------------
-- 声誉（AGG-06）：账户 + 明细流水
-- ---------------------------------------------------------------------------
CREATE TABLE `reputation_account`
(
    `user_id`    VARCHAR(36) NOT NULL COMMENT '用户标识（聚合标识）',
    `score`      INT         NOT NULL COMMENT '声誉分，永不低于 1（INV-R-01）',
    `updated_at` DATETIME(6) NOT NULL,
    `version`    BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='声誉账户（AGG-06）';

CREATE TABLE `reputation_entry`
(
    `id`         VARCHAR(36)  NOT NULL,
    `user_id`    VARCHAR(36)  NOT NULL,
    `event_id`   VARCHAR(160) NOT NULL COMMENT '事件幂等键（INV-R-02）',
    `rule_code`  VARCHAR(32)  NOT NULL COMMENT 'QUESTION_POSTED / ANSWER_SUBMITTED / ANSWER_ACCEPTED / UPVOTE_RECEIVED / DOWNVOTE_RECEIVED / ACCEPT_REVERSED',
    `delta`      INT          NOT NULL,
    `created_at` DATETIME(6)  NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reputation_entry_event` (`event_id`),
    KEY `idx_reputation_entry_user` (`user_id`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='声誉明细（AGG-06，eventId 幂等）';
