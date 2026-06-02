#!/bin/bash
set -e

# ==================== DZ 情侣站 — 一键部署脚本 ====================
# 用法: ./deploy.sh
# 前置条件: 服务器已安装 Docker + Docker Compose

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "========================================"
echo "  DZ 情侣站 — 一键部署"
echo "========================================"

# ---------- 1. 检查依赖 ----------
echo ""
echo "[1/6] 检查环境..."
command -v docker >/dev/null 2>&1 || { echo "❌ 请先安装 Docker: https://docs.docker.com/engine/install/"; exit 1; }
command -v docker compose >/dev/null 2>&1 && DOCKER_COMPOSE="docker compose" || {
  command -v docker-compose >/dev/null 2>&1 && DOCKER_COMPOSE="docker-compose" || {
    echo "❌ 请先安装 Docker Compose"; exit 1;
  }
}
echo "✅ Docker 环境就绪"

# ---------- 2. 配置 .env ----------
echo ""
echo "[2/6] 检查配置文件..."
cd "$SCRIPT_DIR"
if [ ! -f .env ]; then
  if [ -f .env.example ]; then
    echo "⚠️  未找到 .env 文件，正在从 .env.example 创建..."
    cp .env.example .env
    echo "❗ 请编辑 deploy/.env 填入真实配置后重新运行"
    exit 1
  else
    echo "❌ 缺少 .env.example 模板文件"
    exit 1
  fi
fi
source .env
echo "✅ 配置文件就绪"

# ---------- 3. 构建前端 ----------
echo ""
echo "[3/6] 构建前端..."
cd "$PROJECT_DIR/frontend"
if [ ! -f package.json ]; then
  echo "❌ 缺少 frontend/package.json"
  exit 1
fi
npm install --silent
npm run build
echo "✅ 前端构建完成 → frontend/dist/"

# ---------- 4. 构建后端镜像 ----------
echo ""
echo "[4/6] 构建后端 Docker 镜像..."
cd "$SCRIPT_DIR"
$DOCKER_COMPOSE build backend
echo "✅ 后端镜像构建完成"

# ---------- 5. 启动服务 ----------
echo ""
echo "[5/6] 启动全部服务..."
$DOCKER_COMPOSE up -d
echo "✅ 服务已启动"

# ---------- 6. 等待就绪 ----------
echo ""
echo "[6/6] 等待服务就绪..."
echo -n "   等待 MySQL..."
for i in $(seq 1 30); do
  if docker exec dz-mysql mysqladmin ping -h localhost -u root -p"${DB_ROOT_PASSWORD}" --silent 2>/dev/null; then
    echo " ✅"
    break
  fi
  sleep 2
done

echo -n "   等待后端..."
for i in $(seq 1 30); do
  if curl -s -o /dev/null http://localhost:8081/api/health/ping 2>/dev/null; then
    echo " ✅"
    break
  fi
  sleep 3
done

# ---------- 完成 ----------
echo ""
echo "========================================"
echo "  🎉 部署完成！"
echo "========================================"
echo "  访问地址: http://$(curl -s ifconfig.me 2>/dev/null || echo 'YOUR_SERVER_IP')"
echo ""
echo "  常用命令:"
echo "    查看日志:  cd deploy && $DOCKER_COMPOSE logs -f"
echo "    重启服务:  cd deploy && $DOCKER_COMPOSE restart"
echo "    停止服务:  cd deploy && $DOCKER_COMPOSE down"
echo "    更新部署:  ./deploy.sh"
echo "========================================"
