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
    echo "请先安装 Docker Compose"
    exit 1
  }
}
log "Docker 就绪"

# ===== 2. 生成 .env =====
log "准备环境配置..."
cd "$SCRIPT_DIR"

if [ ! -f .env ]; then
  DB_ROOT_PASSWORD=$(openssl rand -base64 24 2>/dev/null || cat /dev/urandom | tr -dc 'a-zA-Z0-9' | head -c 32)
  JWT_SECRET=$(openssl rand -base64 48 2>/dev/null || cat /dev/urandom | tr -dc 'a-zA-Z0-9' | head -c 64)
  CRYPTO_SECRET=$(openssl rand -base64 48 2>/dev/null || cat /dev/urandom | tr -dc 'a-zA-Z0-9' | head -c 64)

  # 从 .env.example 读取 API Key
  ARK_KEY=""; ARK_EP=""
  if [ -f .env.example ]; then
    ARK_KEY=$(grep 'VOLCENGINE_ARK_API_KEY' .env.example | cut -d= -f2-)
    ARK_EP=$(grep 'VOLCENGINE_ARK_ENDPOINT_ID' .env.example | cut -d= -f2-)
  fi

  cat > .env << ENVEOF
# 双人情侣小屋 — 环境变量（自动生成）
DB_ROOT_PASSWORD=${DB_ROOT_PASSWORD}
DB_NAME=couple
JWT_SECRET=${JWT_SECRET}
CRYPTO_SECRET=${CRYPTO_SECRET}
VOLCENGINE_ARK_API_KEY=${ARK_KEY}
VOLCENGINE_ARK_ENDPOINT_ID=${ARK_EP}
COMPOSE_FILE=docker-compose.http.yml
ENVEOF
  log ".env 已自动生成（密钥随机生成，API Key 从 .env.example 读取）"
else
  log ".env 已存在，跳过"
fi

# 备份 .env 到上级目录防止丢失
cp .env "$PROJECT_DIR/.env.deploy.bak" 2>/dev/null || true

# ===== 3. API Key 检查 =====
source .env 2>/dev/null || true
if [ -z "$VOLCENGINE_ARK_API_KEY" ]; then
  warn "未配置 AI API Key，AI 智能管家功能将不可用（其他功能正常）"
fi

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
$DOCKER_COMPOSE -f "$COMPOSE_FILE" build backend --quiet
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
