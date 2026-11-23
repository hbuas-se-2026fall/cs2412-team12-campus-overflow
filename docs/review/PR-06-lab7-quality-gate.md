# PR #6 评审记录 · Lab 7 测试、契约与质量门禁

| 项目 | 内容 |
|---|---|
| PR | 本 Lab 的 PR（`feature/lab7-quality` → `main`） |
| 作者 | 陶浩峰（测试）、陈玉豪（门禁/CI）、倪昊宇（文档） |
| 评审人 | 刘铭洋（`215lmy`）主评 + 陈玉豪（`215cyh`）复核门禁配置 |
| 评审时间 | 第 13 周 · Lab 7 |
| 使用清单 | 团队评审清单 v1.0 全六类 |
| 结论 | 🔴 2 条必须改（已修复）→ **LGTM** |

---

## 评审范围

`pom.xml`、`co-coverage/`、`tools/coverage-gate.mjs`、`config/checkstyle/checkstyle.xml`、
`.github/workflows/ci.yml`、`sonar-project.properties`、全部新增测试、`docs/lab7/**`。

---

## 评审意见（8 条）

### 🔴 1. Surefire 的 `argLine` 覆盖了 JaCoCo agent，覆盖率报告一直是 0

- **位置**：`pom.xml` 的 `maven-surefire-plugin`
- **问题**：原本写死 `<argLine>-Dfile.encoding=UTF-8 …</argLine>`，而 `jacoco:prepare-agent` 是通过
  `argLine` 属性注入 `-javaagent` 的；写死之后 agent 根本没挂上，`target/jacoco.exec` 为空，
  报告里全是 0%——但构建是"成功"的，很容易被误判成"代码就是没覆盖"。
- **建议**：改成 `<argLine>@{argLine} -Dfile.encoding=UTF-8 …</argLine>`，用 late-binding 拼接，
  并在验证记录里贴出"有覆盖率数字"的截图/输出。
- **作者回复**：已修复，`mvn verify` 后各模块 `target/site/jacoco/jacoco.xml` 均有数据。
- **状态**：✅ 已修复

### 🔴 2. 新增的 `co-coverage` 模块没有在父 POM 的 `dependencyManagement` 里声明 `co-bootstrap`

- **位置**：`co-coverage/pom.xml` + 父 POM
- **问题**：父 POM 只管理了 shared/identity/qa/reputation/discovery 的版本，漏了 `co-bootstrap`，
  导致 co-coverage 依赖无版本、整个 reactor 无法解析 POM。
- **建议**：把 `co-bootstrap` 补进 `dependencyManagement`；同时提醒：新增模块时"加依赖管理"要进 PR 检查项。
- **状态**：✅ 已修复

### 🟡 3. Checkstyle 配置路径绑定执行目录，`mvn -f` 会找不到规则文件

- **位置**：父 POM `configLocation`
- **问题**：最初用 `${maven.multiModuleProjectDirectory}`，从别的目录用 `-f` 调用时它指向调用目录，
  而不是仓库根，导致 `Could not find resource …/checkstyle.xml`。
- **建议**：改用 `${session.executionRootDirectory}`，并在 README 明确"**在仓库根目录执行 mvn**"；
  CI 里本来就是 checkout 根目录执行，天然满足。
- **状态**：✅ 已修复

### 🟡 4. Testcontainers 用例本地被跳过，有"永远绿"的风险

- **位置**：`MySqlContainerIntegrationTest`
- **问题**：`disabledWithoutDocker = true` 在无 Docker 的机器上直接 skipped，
  如果有人一直不跑 CI，这个用例可能从来没真正执行过。
- **建议**：① 用例打 `@Tag("testcontainers")`；② CI 的 ubuntu runner 必须执行它，
  并在验证记录里区分"本地 skipped / CI passed"；③ 不要把"本地全绿"当作"集成测试通过"。
- **作者回复**：已加 Tag，验证记录 §5 明确写了本地 skipped 的原因与 CI 上的预期。
- **状态**：✅ 已处理（CI 上真实执行）

### 🟡 5. 契约测试的"接口全覆盖"检查会把 Spring 内置 `/error` 算进来

- **位置**：`OpenApiContractTest#everyImplementedEndpointIsDocumented`
- **问题**：`RequestMappingHandlerMapping` 里包含 `/error`，它不是业务接口，不该出现在 openapi.yaml。
- **建议**：过滤 `/error`；同时保留"文档里有、代码里没有"的反向检查（本次未做，记入 Sprint 4）。
- **状态**：✅ 已过滤；反向检查已记录为后续项

### 🟡 6. 测试用 `Instant.now()` 会随机器时间漂移

- **位置**：多个应用服务测试
- **问题**：应用服务的限流（60 秒）与时间相关，用真实时钟会让测试偶发失败。
- **建议**：一律用 `Clock.fixed(...)` 注入；断言时间字段时用固定值。
- **作者回复**：所有新增测试都用 `Clock.fixed`，仓库里已无 `Instant.now()` 直接出现在业务测试中。
- **状态**：✅ 已修复

### 🟢 7. 用 `ArgumentCaptor` 验证"发事件"这一副作用

- **评价**：投票测试捕获 `eventPublisher.publish(...)` 的参数，断言 `VoteCast.reputationDelta`
  是 +10 / −12 / −10，而不是只断言"方法被调用"——这正是 Lab 7 Step 1 要求的做法。

### 🟢 8. 覆盖率门禁拆成"整体 + 核心包"两级

- **评价**：整体 ≥70% 防止"只测样板代码"，`domain/model` ≥90% 防止"核心逻辑裸奔"；
  而且没有为了让数字好看去测 getter，而是补了真实行为（限流、改判、幂等）。

---

## 结论

两条 🔴 都会让"门禁看起来生效、实际没生效"——是最危险的一类问题。
修复后，`mvn verify` 同时产出测试结果、覆盖率、Checkstyle 与聚合报告，CI 也接上了。
**同意合并（LGTM）**。

> 评审心得：**质量门禁本身也要被测试**。这次就是靠"报告里为什么是 0%"才发现 agent 没挂上。
