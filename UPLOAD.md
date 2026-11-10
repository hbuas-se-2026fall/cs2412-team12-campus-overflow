# Lab 4 提交说明（给组长 / SM 执行）

> 本目录就是可直接提交的 Lab 4 产物：**根目录 = 团队仓库根目录**（`docs/` 与 Maven 工程同级）。
> 本机网络无法访问 github.com，所以代码与文档已经准备好，推送这一步需要你在能上网的环境执行。

---

## 1. 要提交什么

| 提交物 | 文件 / 目录 | 说明 |
|---|---|---|
| ① 多模块 Maven 工程 | 根 `pom.xml` + `co-*/` 六个模块 | `mvn clean install` 已全绿（44 个测试） |
| ② 数据库配置 | `co-bootstrap/src/main/resources/application.yml` + `db/migration/V1__init_schema.sql` | Flyway 自动建表 |
| ③ ADR | `docs/adr/ADR-002-package-structure.md` | 分模块与分层决策 |
| ④ Postman Collection | `docs/api/CampusOverflow-Lab4.postman_collection.json` | 含 `POST /api/v1/questions` 成功用例 |
| 附录 | `docs/lab4/验证记录.md` | 真实 MySQL 8.4 的端到端证据 |
| 说明 | `README.md` | 团队、快速开始、API、设计要点、已知边界 |

**不要提交**：`target/`（已在 `.gitignore` 里忽略）、任何含有本机数据库密码的本地配置文件。

---

## 2. 推送步骤

```bash
# 0) 克隆团队仓库（如已克隆则跳过）
git clone https://github.com/hbuas-se-2026fall/cs2412-team12-campus-overflow.git
cd cs2412-team12-campus-overflow

# 1) 拉最新 main，确保不落后
git checkout main
git pull origin main

# 2) 开分支（分支命名按团队章程：feature/<姓名拼音>-<主题>）
git checkout -b feature/lab4-backend

# 3) 把本目录内容复制到仓库根（保留已有 docs/backlog、docs/ddd 等 Lab 1-3 产物）
#    Windows 示例（注意：只复制 pom.xml、co-*、docs/adr、docs/api、docs/lab4、README.md、.gitignore）
robocopy "D:\dsh-output\lab4-backend" "D:\path\to\cs2412-team12-campus-overflow" /E /XD target .git /XF "*.log"

# 4) 检查改动，确认没有把 target/ 或密码带进来
git status
git diff --stat

# 5) 提交（commit message 按 Lab 1 团队章程要求带全员签名）
git add .
git commit -m "feat(lab4): 六模块 DDD 后端骨架 + POST /api/v1/questions 端到端贯通

- 父 POM 统一 Spring Boot 3.5.6 / JDK 21 / JUnit 5
- 新增 co-shared-kernel/co-identity/co-qa/co-reputation/co-discovery/co-bootstrap
- Question/Answer/Vote 聚合保护 INV-Q/A/V 不变式，领域层零框架依赖
- JPA 持久化模型与领域模型分离；Flyway V1 建 user/question/answer
- @RestControllerAdvice 统一错误响应 + Logback JSON 日志 + traceId
- docs/adr/ADR-002 记录分模块决策；Postman 集合含成功用例
- 验证：mvn clean install 44 个测试全绿；MySQL 8.4 实测见 docs/lab4/验证记录.md

Signed-off-by: 倪昊宇 (PO) <...>
Signed-off-by: 陶浩峰 (Dev) <2735514862@qq.com>
Signed-off-by: 程靖轩 (Dev) <...>
Signed-off-by: 陈玉豪 (SM) <...>
Signed-off-by: 刘铭洋 (SM) <...>"

# 6) 推送并发起 PR
git push -u origin feature/lab4-backend
```

---

## 3. PR 描述（可直接粘贴）

```markdown
## 这个 PR 做了什么

Lab 4 后端工程骨架与领域代码：把 Lab 3 的 6 个限界上下文落成 6 个 Maven 模块，
并打通第一个端到端接口 `POST /api/v1/questions`。

**关联故事**：US-04（发布问题，Sprint 2）；顺带完成 US-01 的注册/验证状态机。
**关联文档**：`docs/adr/ADR-002-package-structure.md`、`docs/lab4/验证记录.md`

## 怎么验证

```bash
mvn clean install          # 44 个测试全绿（38 聚合单元 + 6 接口集成）
mysql -uroot -p -e "CREATE DATABASE campus_overflow DEFAULT CHARACTER SET utf8mb4"
java -jar co-bootstrap/target/campus-overflow.jar
# 然后用 docs/api/ 里的 Postman 集合跑 01 → 02 两个文件夹
```

## 评审重点（按 ADR-002 §12 的 6 条清单）

- [ ] `domain` 包内没有任何 Spring / JPA import
- [ ] 聚合没有 setter，状态只能通过业务方法改变
- [ ] 跨上下文只传 `UserId`，问答上下文没有 import `co-identity`
- [ ] 每条业务规则都能对应到 `INV-*` 编号
- [ ] `V1__init_schema.sql` 与实际写入一致（见验证记录第 6 节）
- [ ] 新用例都有聚合级单元测试

## 已知边界（故意不做，避免范围蔓延）

登录鉴权、问题列表、回答接口、投票接口、标签检索、声誉持久化、治理，均按 ADR-002 §13 的演进表排到 Sprint 2 末 / Sprint 3 / Sprint 4。

## 截图 / 证据

- `mvn clean install` 输出：验证记录 §1
- Flyway 建表日志：§2
- 接口 201/200 响应：§3
- MySQL `SELECT` 结果：§6
```

---

## 4. 本次没有自动完成的事（需要人来做）

| 事项 | 原因 | 建议做法 |
|---|---|---|
| `git push` / 开 PR | 本机无法访问 github.com（网络受限） | 按上面第 2、3 节执行 |
| 截图 | 截图需要人眼看界面 | Postman 跑完 01/02 文件夹后截图，贴到 PR 与实验报告 |
| 看板卡片移到 Done | 看板在 GitHub Projects | PR 合并后由 SM 更新 |
| Lab 3 的 ADR-001/003 | Lab 4 只要求 ADR-002 | 按 Lab 3 §6 待办排期 |
