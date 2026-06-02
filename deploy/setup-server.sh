#!/bin/bash
set -e

# ==================== 双人情侣小屋 — 服务器环境安装 ====================
# 不依赖 Docker Hub，全部用 apt + 本地 JAR 部署
# 用法: chmod +x setup-server.sh && ./setup-server.sh

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BOLD='\033[1m'; NC='\033[0m'
log()  { echo -e "${GREEN}[+]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }

echo ""
echo -e "${BOLD}========================================${NC}"
echo -e "${BOLD}   双人情侣小屋 环境安装${NC}"
echo -e "${BOLD}========================================${NC}"
echo ""

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# ===== 1. 安装基础依赖 =====
log "安装 Java / Nginx / Redis / MySQL..."
apt-get update -qq
apt-get install -y -qq openjdk-8-jre-headless nginx redis-server mysql-server 2>&1 | tail -1

# ===== 2. 配置 MySQL =====
log "配置 MySQL..."
if ! mysql -u root -e "SELECT 1" 2>/dev/null; then
  # Ubuntu 默认用 auth_socket，改成密码认证
  mysql -u root --socket=/var/run/mysqld/mysqld.sock << 'EOSQL' 2>/dev/null || true
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '';
FLUSH PRIVILEGES;
EOSQL
fi

# 建库建表
DB_PASS=$(openssl rand -base64 18 2>/dev/null | tr -dc 'a-zA-Z0-9' | head -c 20)
mysql -u root << EOSQL
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '${DB_PASS}';
CREATE DATABASE IF NOT EXISTS couple DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
FLUSH PRIVILEGES;
EOSQL
mysql -u root -p"${DB_PASS}" couple < "$SCRIPT_DIR/init-db.sql"
log "MySQL 数据库 couple 已创建"

# ===== 3. 生成密钥 =====
log "生成安全配置..."
JWT_SECRET=$(openssl rand -base64 48 2>/dev/null | tr -dc 'a-zA-Z0-9' | head -c 64)
CRYPTO_SECRET=$(openssl rand -base64 48 2>/dev/null | tr -dc 'a-zA-Z0-9' | head -c 64)

# ===== 4. API Key 交互式配置 =====
ARK_KEY=""; ARK_EP=""
if [ -f .env ]; then source .env 2>/dev/null || true; ARK_KEY="$VOLCENGINE_ARK_API_KEY"; ARK_EP="$VOLCENGINE_ARK_ENDPOINT_ID"; fi
case "$ARK_KEY" in ""|"your-ark-api-key-here"|"sk-your-api-key-here")
  echo ""
  echo "  需要火山引擎 Ark API Key（可选，跳过则 AI 降级为规则模式）"
  read -p "  API Key (ark-开头): " ARK_KEY
  [ -n "$ARK_KEY" ] && read -p "  Endpoint ID (ep-开头): " ARK_EP
  ;;
esac

# 保存 .env
cat > .env << ENVEOF
DB_HOST=localhost
DB_PORT=3306
DB_NAME=couple
DB_USER=root
DB_PASSWORD=${DB_PASS}
REDIS_HOST=localhost
REDIS_PORT=6379
JWT_SECRET=${JWT_SECRET}
CRYPTO_SECRET=${CRYPTO_SECRET}
VOLCENGINE_ARK_API_KEY=${ARK_KEY}
VOLCENGINE_ARK_ENDPOINT_ID=${ARK_EP}
ENVEOF
log ".env 已生成"

# ===== 5. 部署后端 JAR =====
log "部署后端..."
mkdir -p /opt/dz-community/backend
if [ -f "$SCRIPT_DIR/../backend/target/couple-backend-0.0.1-SNAPSHOT.jar" ]; then
  # 从项目目录复制（已构建好的）
  cp "$SCRIPT_DIR/../backend/target/couple-backend-0.0.1-SNAPSHOT.jar" /opt/dz-community/backend/app.jar
else
  warn "未找到 JAR 文件，请先将 couple-backend-0.0.1-SNAPSHOT.jar 上传到 /opt/dz-community/backend/app.jar"
  warn "本地构建: cd backend && mvnw clean package -DskipTests"
fi

# systemd 服务
cat > /etc/systemd/system/dz-community.service << 'UNITEOF'
[Unit]
Description=双人情侣小屋 后端服务
After=network.target mysql.service redis-server.service
Wants=mysql.service redis-server.service

[Service]
User=root
WorkingDirectory=/opt/dz-community/backend
EnvironmentFile=/opt/DZ-Community/deploy/.env
ExecStart=/usr/bin/java -jar /opt/dz-community/backend/app.jar --spring.profiles.active=prod
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
UNITEOF
systemctl daemon-reload
systemctl enable dz-community

# ===== 6. 部署前端 =====
log "部署前端..."
rm -rf /opt/dz-community/frontend
cp -r "$PROJECT_DIR/frontend/dist" /opt/dz-community/frontend
chown -R www-data:www-data /opt/dz-community/frontend

# ===== 7. 配置 Nginx =====
cat > /etc/nginx/sites-available/dz-community << NGEOF
server {
    listen 80;
    server_name _;
    client_max_body_size 50m;

    root /opt/dz-community/frontend;
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_read_timeout 120s;
    }

    location /ws/ {
        proxy_pass http://127.0.0.1:8081;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host \$host;
        proxy_read_timeout 86400s;
    }

    location /uploads/ {
        proxy_pass http://127.0.0.1:8081;
    }

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css text/xml text/javascript application/javascript application/json image/svg+xml;
}
NGEOF
ln -sf /etc/nginx/sites-available/dz-community /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default
systemctl enable nginx

# ===== 8. 启动全部服务 =====
log "启动服务..."
systemctl restart mysql redis-server nginx
systemctl start dz-community

# 等待后端就绪
log "等待后端就绪..."
for i in $(seq 1 20); do
  if curl -s -o /dev/null http://127.0.0.1:8081/api/health/ping 2>/dev/null; then
    log "后端启动成功 ✓"
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
echo "  访问地址: http://$(curl -s ifconfig.me 2>/dev/null || echo 'YOUR_IP')"
echo ""
echo "  常用命令:"
echo "    后端日志:   journalctl -u dz-community -f"
echo "    重启后端:   systemctl restart dz-community"
echo "    重启全部:   systemctl restart dz-community nginx mysql redis-server"
echo ""
