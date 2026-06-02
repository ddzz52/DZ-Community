#!/bin/bash
set -e

# ==================== 双人情侣小屋 社区版 — 一键部署脚本 ====================
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
NC='\033[0m'

log()  { echo -e "${GREEN}[+]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }

echo ""
echo -e "${BOLD}========================================${NC}"
echo -e "${BOLD}   双人情侣小屋 社区版 — 部署${NC}"
echo -e "${BOLD}========================================${NC}"
echo ""

# ===== 1. 检查 Docker =====
log "检查 Docker 环境..."
command -v docker >/dev/null 2>&1 || {
  echo "请先安装 Docker: curl -fsSL https://get.docker.com | bash"
  exit 1
}
DOCKER_COMPOSE="docker compose"
command -v "docker compose" >/dev/null 2>&1 || {
  command -v docker-compose >/dev/null 2>&1 && DOCKER_COMPOSE="docker-compose" || {
    echo "请先安装 Docker Compose: apt-get install -y docker-compose-plugin"
    exit 1
  }
}
log "Docker 就绪"

# ===== 2. 交互式配置 API Key =====
log "配置 AI API Key..."
cd "$SCRIPT_DIR"

ARK_KEY=""; ARK_EP=""

# 从已有 .env 或 .env.example 读取
if [ -f .env ]; then
  source .env 2>/dev/null || true
  ARK_KEY="$VOLCENGINE_ARK_API_KEY"
  ARK_EP="$VOLCENGINE_ARK_ENDPOINT_ID"
  # 旧版 .env 可能缺少字段，自动升级
  if [ -z "$CRYPTO_SECRET" ] || [ -z "$COMPOSE_FILE" ]; then
    log "检测到旧版 .env，自动升级..."
    rm -f .env
  fi
elif [ -f .env.example ]; then
  ARK_KEY=$(grep 'VOLCENGINE_ARK_API_KEY' .env.example | cut -d= -f2-)
  ARK_EP=$(grep 'VOLCENGINE_ARK_ENDPOINT_ID' .env.example | cut -d= -f2-)
fi

# 如果 API Key 是占位符或为空，提示输入
NEED_KEY=false
case "$ARK_KEY" in
  ""|"your-ark-api-key-here"|"sk-your-api-key-here"|"your-api-key-here") NEED_KEY=true ;;
esac

if $NEED_KEY; then
  echo ""
  echo "  需要火山引擎 Ark API Key 才能使用 AI 智能管家功能。"
  echo "  从 https://console.volcengine.com/ark 获取，格式为 ark- 开头。"
  echo "  如果暂不配置，直接回车跳过（AI 功能自动降级为规则模式）。"
  echo ""
  read -p "  API Key: " ARK_KEY
  if [ -n "$ARK_KEY" ]; then
    read -p "  Endpoint ID (ep-开头): " ARK_EP
    log "AI API Key 已配置"
  else
    warn "跳过 AI 配置（其他功能正常使用）"
    ARK_KEY=""
    ARK_EP=""
  fi
else
  log "API Key 已配置"
fi

# ===== 3. 生成 .env =====
log "生成安全密钥..."

DB_ROOT_PASSWORD=$(openssl rand -base64 24 2>/dev/null || cat /dev/urandom | tr -dc 'a-zA-Z0-9' | head -c 32)
JWT_SECRET=$(openssl rand -base64 48 2>/dev/null || cat /dev/urandom | tr -dc 'a-zA-Z0-9' | head -c 64)
CRYPTO_SECRET=$(openssl rand -base64 48 2>/dev/null || cat /dev/urandom | tr -dc 'a-zA-Z0-9' | head -c 64)

cat > .env << ENVEOF
# 双人情侣小屋 — 环境变量（自动生成 $(date '+%Y-%m-%d %H:%M')）
DB_ROOT_PASSWORD=${DB_ROOT_PASSWORD}
DB_NAME=couple
JWT_SECRET=${JWT_SECRET}
CRYPTO_SECRET=${CRYPTO_SECRET}
VOLCENGINE_ARK_API_KEY=${ARK_KEY}
VOLCENGINE_ARK_ENDPOINT_ID=${ARK_EP}
COMPOSE_FILE=docker-compose.http.yml
ENVEOF
log ".env 已生成（密钥随机生成）"

cp .env "$PROJECT_DIR/.env.deploy.bak" 2>/dev/null || true

# ===== 4. 构建前端 =====
log "构建前端..."
cd "$PROJECT_DIR/frontend"
if ! command -v npm >/dev/null 2>&1; then
  warn "未安装 Node.js，跳过前端构建。请先在本地 npm run build，将 dist/ 上传到服务器。"
  warn "或者在此服务器安装 Node.js: curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && apt-get install -y nodejs"
else
  if [ ! -d node_modules ]; then
    npm install --silent
  fi
  npm run build
  log "前端构建完成"
fi

# ===== 5. 构建后端镜像 =====
log "构建后端 Docker 镜像..."
cd "$SCRIPT_DIR"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.http.yml}"
$DOCKER_COMPOSE -f "$COMPOSE_FILE" build backend
log "后端镜像构建完成"

# ===== 6. 启动服务 =====
log "启动全部服务..."
$DOCKER_COMPOSE -f "$COMPOSE_FILE" up -d

# ===== 7. 等待就绪 =====
log "等待 MySQL 就绪..."
for i in $(seq 1 30); do
  if docker exec dz-mysql mysqladmin ping -h localhost -u root --silent 2>/dev/null; then
    echo "  MySQL ✓"
    break
  fi
  sleep 2
done

log "等待后端就绪..."
for i in $(seq 1 30); do
  if curl -s -o /dev/null http://localhost:8081/api/health/ping 2>/dev/null; then
    echo "  后端 ✓"
    break
  fi
  sleep 3
done

# ===== 完成 =====
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  部署完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "  访问地址: http://$(curl -s ifconfig.me 2>/dev/null || curl -s ip.sb 2>/dev/null || echo 'YOUR_IP')"
echo ""
echo "  常用命令:"
echo "    查看日志:  cd deploy && $DOCKER_COMPOSE -f $COMPOSE_FILE logs -f"
echo "    重启服务:  cd deploy && $DOCKER_COMPOSE -f $COMPOSE_FILE restart"
echo "    停止服务:  cd deploy && $DOCKER_COMPOSE -f $COMPOSE_FILE down"
echo ""
echo "  数据备份:"
echo "    MySQL:  docker exec dz-mysql mysqldump -u root couple > backup.sql"
echo "    上传文件: docker cp dz-backend:/app/uploads ./uploads_backup"
echo ""
echo "  安全提醒:"
echo "    deploy/.env 包含密钥，请勿泄露。已备份至项目根目录 .env.deploy.bak"
echo "    建议配置防火墙仅开放 80 端口"
echo ""
