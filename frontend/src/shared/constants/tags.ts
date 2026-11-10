/**
 * 标签词表（SK-01）：必须与后端 `application.yml` 的
 * `campus-overflow.tags.vocabulary` 保持一致，否则提交会被 INV-Q-02 拒绝。
 *
 * Sprint 3 的 US-10 会改成从后端拉取词表，这里的常量只是过渡。
 */
export const TAG_OPTIONS: readonly string[] = [
  'java',
  'spring-boot',
  'spring-data-jpa',
  'mysql',
  'flyway',
  'ddd',
  'maven',
  'junit',
  'docker',
  'git',
  'react',
  'typescript',
  '数据库',
  '算法',
  '校园生活',
  '选课',
  '考研',
  '实习',
  '保研',
  '实验室',
];
