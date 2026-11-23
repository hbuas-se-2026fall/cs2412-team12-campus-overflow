#!/usr/bin/env node
/**
 * Lab 7 覆盖率质量门禁。
 *
 * 读取 co-coverage 模块聚合出的 jacoco.xml（所有模块的执行数据合并），校验：
 *   1. 整体行覆盖率 ≥ 70%
 *   2. 每个核心聚合包（*\/domain\/model）行覆盖率 ≥ 90%
 *
 * 用法：
 *   node tools/coverage-gate.mjs [jacoco.xml 路径]
 *
 * 退出码：0 = 通过；1 = 未达标（CI 会因此失败）。
 */
import { readFileSync } from 'node:fs';

const REPORT = process.argv[2] ?? 'co-coverage/target/site/jacoco-aggregate/jacoco.xml';

const THRESHOLDS = {
  overall: 70,
  domainModel: 90,
};

/** 取一段 XML 里最后一个 LINE counter：JaCoCo 会把汇总 counter 放在最后。 */
function lineCounter(block) {
  const matches = [...block.matchAll(/<counter type="LINE" missed="(\d+)" covered="(\d+)"\/>/g)];
  if (matches.length === 0) {
    return null;
  }
  const last = matches[matches.length - 1];
  const missed = Number(last[1]);
  const covered = Number(last[2]);
  return { missed, covered, total: missed + covered };
}

function percentage(counter) {
  if (!counter || counter.total === 0) {
    return 100;
  }
  return (100 * counter.covered) / counter.total;
}

function format(percent) {
  return `${percent.toFixed(1)}%`;
}

const xml = readFileSync(REPORT, 'utf8');

const overall = lineCounter(xml);
if (!overall) {
  console.error(`✖ 无法从 ${REPORT} 读取 LINE counter`);
  process.exit(1);
}

const packages = [...xml.matchAll(/<package name="([^"]+)"[^>]*>([\s\S]*?)<\/package>/g)]
  .map((match) => ({ name: match[1], counter: lineCounter(match[2]) }))
  .filter((pkg) => pkg.counter && pkg.counter.total > 0 && pkg.name.endsWith('/domain/model'));

const overallPercent = percentage(overall);
const failures = [];

console.log('== 覆盖率门禁（Lab 7）==');
console.log(`报告：${REPORT}`);
console.log(
  `整体行覆盖率：${format(overallPercent)}（${overall.covered}/${overall.total}），阈值 ${THRESHOLDS.overall}%`,
);
if (overallPercent < THRESHOLDS.overall) {
  failures.push(`整体覆盖率 ${format(overallPercent)} < ${THRESHOLDS.overall}%`);
}

console.log('\n核心聚合包（domain/model）：');
for (const pkg of packages) {
  const percent = percentage(pkg.counter);
  const ok = percent >= THRESHOLDS.domainModel;
  console.log(
    `  ${ok ? '✔' : '✖'} ${pkg.name.replaceAll('/', '.')}：${format(percent)}（${pkg.counter.covered}/${pkg.counter.total}）`,
  );
  if (!ok) {
    failures.push(`${pkg.name} ${format(percent)} < ${THRESHOLDS.domainModel}%`);
  }
}
if (packages.length === 0) {
  failures.push('没有找到任何 domain/model 包的覆盖率数据');
}

if (failures.length > 0) {
  console.error('\n✖ 质量门禁未通过：');
  for (const failure of failures) {
    console.error(`  - ${failure}`);
  }
  process.exit(1);
}

console.log('\n✔ 质量门禁通过：整体 ≥70%，核心聚合包 ≥90%。');
