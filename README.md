# TradeStation — 多店铺电商平台

全栈微服务电商系统：买家端 + 卖家端 + 客服系统（预留 AI 客服接入）

## 技术栈

| 层级 | 技术 |
|---|---|
| 后端 | Java 17, Spring Boot 2.7.18, Spring Cloud 2021.0.9, Spring Cloud Alibaba 2021.0.6.0 |
| 注册中心 | Nacos 2.2.3 |
| 网关 | Spring Cloud Gateway |
| 熔断降级 | Sentinel 2.2.7 |
| 链路跟踪 | Zipkin 2.24 + Sleuth 3.1.9 |
| 分布式事务 | Seata 1.6.1 (AT 模式) |
| 消息队列 | RabbitMQ 3.12 |
| 数据库 | MySQL 8.0 (每微服务独立 schema) |
| ORM | MyBatis-Plus 3.5.3.1 |
| 前端 | Vue 3.3 + TypeScript + Vite 5 + Element Plus 2.5 |
| 容器化 | Docker Compose |

## 项目结构

```
TradeStation/
├── pom.xml                    # 父 POM，统一版本管理
├── common/                    # 公共模块：统一返回体、异常、JWT、配置
├── user-service/              # 用户服务 (8081)
├── shop-service/              # 店铺 + 客服消息服务 (8082)
├── product-service/           # 商品 + 库存服务 (8083)
├── order-service/             # 订单服务 (8084)
├── payment-service/           # 支付服务 (8085)
├── gateway-service/           # API 网关 (8080)
├── sql/                       # 数据库 DDL（5 个独立 schema）
├── api-specs/                 # OpenAPI 3.0 接口文档
├── nacos-config/              # Nacos 配置中心示例配置
├── web-ui/                    # Vue3 前端 (3000)
└── docker/                    # Docker Compose + Dockerfile
```

## 快速启动

### 1. 启动基础设施

```bash
cd docker
docker-compose up -d mysql nacos rabbitmq sentinel-dashboard zipkin seata-server
```

等待 MySQL 和 Nacos 就绪（约 30 秒）。

### 2. 初始化数据库

所有 SQL 文件位于 `sql/` 目录，MySQL 容器启动时会自动执行。
也可手动导入：

```bash
docker exec -i ts-mysql mysql -uroot -proot < sql/db_user.sql
docker exec -i ts-mysql mysql -uroot -proot < sql/db_shop.sql
docker exec -i ts-mysql mysql -uroot -proot < sql/db_product.sql
docker exec -i ts-mysql mysql -uroot -proot < sql/db_order.sql
docker exec -i ts-mysql mysql -uroot -proot < sql/db_payment.sql
```

### 3. 配置 Nacos

访问 http://localhost:8848/nacos，将 `nacos-config/` 中的配置文件导入：
- `common-config.yaml` → 共享配置（所有服务）
- `user-service.yaml` → user-service 数据源
- `shop-service.yaml` → shop-service 数据源
- `product-service.yaml` → product-service 数据源
- `order-service.yaml` → order-service 数据源
- `payment-service.yaml` → payment-service 数据源
- `gateway-service.yaml` → 网关路由 + JWT 配置

### 4. 构建并启动微服务

```bash
mvn clean package -DskipTests
cd docker
docker-compose up -d gateway-service user-service shop-service product-service order-service payment-service
```

### 5. 启动前端

```bash
cd web-ui
npm install
npm run dev
```

或 Docker 方式：
```bash
docker-compose up -d web-ui
```

前端开发地址：http://localhost:3000

## API 文档

完整的 OpenAPI 3.0 规范位于 `api-specs/` 目录：
- `user-service-api.yaml` — 用户注册/登录/地址
- `shop-service-api.yaml` — 店铺管理/客服消息/AI 预留
- `product-service-api.yaml` — 商品/SKU/库存
- `order-service-api.yaml` — 订单创建/状态流转
- `payment-service-api.yaml` — 支付单/模拟回调

## 微服务表

| 服务 | 端口 | 数据库 | 核心功能 |
|---|---|---|---|
| gateway-service | 8080 | — | 路由转发、JWT 鉴权、CORS、Sentinel 限流 |
| user-service | 8081 | db_user | 用户注册登录、收货地址 CRUD |
| shop-service | 8082 | db_shop | 店铺入驻、客服消息（含 AI 预留） |
| product-service | 8083 | db_product | 商品发布、SKU 管理、库存（乐观锁扣减） |
| order-service | 8084 | db_order | 订单创建、取消、Feign 远程调用 |
| payment-service | 8085 | db_payment | 支付单生成、模拟支付回调 |

## AI 客服预留

客服消息表 `t_customer_message` 设计为双字段区分：

| 字段 | 含义 | 取值 |
|---|---|---|
| `sender_type` | 发送人类型 | BUYER / SELLER / **AI** |
| `message_type` | 消息渠道 | CUSTOMER（人工）/ **AI**（AI 客服） |

内部接口 `POST /internal/shop/customer/message/ai` 专供未来 AI 模块调用，自动写入 `sender_type=AI, message_type=AI`。
前端聊天窗口已预留 AI 消息样式（蓝色背景）。

## 基础设施地址

| 组件 | 地址 |
|---|---|
| Nacos Console | http://localhost:8848/nacos |
| Sentinel Dashboard | http://localhost:8858 |
| Zipkin UI | http://localhost:9411 |
| RabbitMQ Management | http://localhost:15672 |
| Seata Server | localhost:8091 |
