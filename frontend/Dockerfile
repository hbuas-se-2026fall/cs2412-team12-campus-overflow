# syntax=docker/dockerfile:1.7
# ============================================================================
# CampusOverflow 前端镜像（Lab 8）
#   builder: node 20 + pnpm 构建静态资源
#   runtime: nginx alpine，反代 /api 到后端，SPA 路由 fallback
# 构建（在 frontend/ 目录）：
#   docker build -t campus-overflow/frontend:latest .
# ============================================================================
FROM node:20-alpine AS build
# 固定 pnpm 主版本，避免 corepack 在离线/受限网络下拉取 latest
RUN npm i -g pnpm@9
WORKDIR /app

COPY package.json pnpm-lock.yaml ./
# --ignore-scripts：跳过 prepare（安装 git hooks），镜像里没有 .git
RUN pnpm install --frozen-lockfile --ignore-scripts

COPY . .
RUN pnpm build

# ---------------------------------------------------------------------------
FROM nginx:1.27-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -qO- http://127.0.0.1/healthz >/dev/null || exit 1
