# 双人情侣小屋 · 社区版

> 开源情侣智能生活平台，支持多用户，一键 Docker 部署。

基于大模型 **AI 智能管家** 的一站式情侣空间，支持自然语言交互完成记账、日记、纪念日、心愿清单、天气查询、约会规划等 19 个业务场景；同时包含实时聊天、相册、备忘录、经期关怀等完整功能模块。

## 与原版 DZ 的区别

| | DZ 原版 | 本社区版 |
|---|---|---|
| 用户数量 | 仅限 2 人 | **不限量，每对情侣独立空间** |
| 注册方式 | 直接注册 | 支持邀请码配对 |
| 视觉风格 | 粉紫色系 | 靛蓝 + 紫罗兰高级中性风 |
| 部署方式 | 手动 | **Docker Compose 一键部署** |
| Logo | DZ 文字 | 小屋图形 |

## 技术栈

| 层级 | 技术 |
|---|---|
| 前端 | Vue 3 · Vite · Element Plus · Pinia · Axios · SockJS |
| 后端 | Spring Boot 2.7 · MyBatis · MySQL 8.0 · Redis · WebSocket |
| AI Agent | 火山方舟 Ark API (豆包 doubao-pro-32k) · Function Calling |
| 部署 | Docker Compose · Nginx · 阿里云 ECS |

## 功能模块

### AI 智能管家
- 36 种意图识别，4 层混合架构：Redis 缓存 → 关键词快速匹配 → LLM Function Calling → 规则兜底
- 31 个工具调用：记账 / 日记 / 纪念日 / 心愿 / 相册 / 经期 / 天气 / 约会规划 / 旅行攻略
- 三层记忆系统：长期记忆 · 中期情景记忆 · 短期上下文
- 断路器 + 指数退避重试，LLM 不可用时自动降级为规则模式

### 实时聊天
- WebSocket 双向通信（Spring WebSocket + STOMP）
- 消息已读 / 撤回 / 删除 / 批量删除 / 清空

### 业务模块
| 模块 | 功能 |
|---|---|
| 纪念日 | 公历 / 农历双历支持，倒计时，置顶，封面图 |
| 日记 | 富文本、心情、私密标记、点赞 / 收藏 / 评论 / 附件 |
| 相册 | 上传、缩略图、相册管理、软删除回收站 |
| 记账 | 分类统计、月度汇总、饼图/柱图可视化 |
| 备忘录 | 分类管理、完成标记、实时同步 |
| 心愿清单 | 优先级、状态追踪、刮刮乐卡片、大转盘抽选 |
| 经期关怀 | 加密数据存储、周期预测、提醒 |

### 多用户注册
- 不限总量，每对情侣独立空间
- 注册后生成 8 位邀请码，分享给伴侣即可加入同一空间
- 每空间上限 2 人

## 一键部署

### 环境要求
- 服务器：1 核 2G 以上，Linux（推荐 Ubuntu 20.04+ / CentOS 7+）
- Docker + Docker Compose

### 部署步骤

```bash
# 1. 克隆项目
git clone https://github.com/你的用户名/couple-cottage-community.git
cd couple-cottage-community/deploy

# 2. 配置 API Key（可选，不配置则 AI 功能自动降级为规则模式）
cp .env.example .env
# 编辑 .env，填入火山方舟 Ark API Key 和 Endpoint ID

# 3. 一键部署
chmod +x deploy-community.sh
./deploy-community.sh
```

脚本自动完成：密钥生成 → 前端构建 → 后端镜像构建 → MySQL/Redis/Nginx/Backend 启动。

浏览器访问 `http://你的服务器IP` 即可使用。

### 常用命令

```bash
cd deploy

# 查看日志
docker compose -f docker-compose.http.yml logs -f

# 重启
docker compose -f docker-compose.http.yml restart

# 停止
docker compose -f docker-compose.http.yml down

# 数据备份
docker exec dz-mysql mysqldump -u root couple > backup.sql

# 更新代码后重建
docker compose -f docker-compose.http.yml up -d --build backend
```

## 本地开发

```bash
# 1. 启动 MySQL 和 Redis
docker compose -f deploy/docker-compose.http.yml up -d mysql redis

# 2. 初始化数据库
mysql -u root -h 127.0.0.1 -p couple < deploy/init-db.sql

# 3. 启动后端（IDEA 配置环境变量后直接运行 CoupleApplication.java）

# 4. 启动前端
cd frontend
npm install && npm run dev
```

## 项目结构

```
couple-cottage-community/
├── frontend/              # Vue 3 前端
│   └── src/
│       ├── api/           # Axios 封装
│       ├── modules/       # 功能模块（agent/chat/period/accounts/wish/wishlist）
│       ├── router/        # Vue Router
│       ├── stores/        # Pinia 状态管理
│       └── views/         # 页面视图
│
├── backend/               # Spring Boot 后端
│   └── src/main/
│       ├── java/com/dz/couple/
│       │   ├── common/    # 统一返回 / 异常 / 拦截器
│       │   ├── config/    # 配置类
│       │   └── module/    # 21 个业务模块
│       └── resources/     # 配置文件
│
└── deploy/                # 部署相关
    ├── Dockerfile
    ├── docker-compose.http.yml   # HTTP 快速部署
    ├── docker-compose.yml        # HTTPS 生产部署
    ├── nginx.http.conf
    ├── nginx.conf
    ├── init-db.sql
    ├── deploy-community.sh       # 一键部署脚本
    └── .env.example
```

## AI Agent 架构

```
用户消息
  → ① Redis 缓存（命中率 60%+，5min TTL）
  → ② 关键词快速匹配（高频场景，0 LLM 成本）
  → ③ LLM Function Calling（复杂语义，31 Tools）
  → ④ 规则链兜底（LLM 不可用时全覆盖）
  → 返回意图 + 结构化参数 → 执行 Handler
```

## License

MIT

---

**注意**：本项目为社区开源版，仅供学习与个人使用。如需商业使用请联系作者。
