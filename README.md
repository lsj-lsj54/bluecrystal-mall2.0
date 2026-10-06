# 蓝水晶商城 v2（重构骨架）

基于 **Spring Boot 3.5 + Spring Cloud 2025.0 + Spring Cloud Alibaba 2025.0** 的商城微服务骨架：
网关统一鉴权、商品、购物车、用户、订单、余额支付。

> 这是从旧版（Spring Boot 2.7 / Spring Cloud 2021 / JDK 17）重构出的**全新 v2 骨架**，
> 版本选型与核实依据见 [docs/VERSION-MATRIX.md](docs/VERSION-MATRIX.md)，
> 调用链与鉴权设计见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)。

## 技术栈

| 组件 | 版本 |
|------|------|
| JDK | 21 |
| Spring Boot | 3.5.16 |
| Spring Cloud | 2025.0.3 |
| Spring Cloud Alibaba | 2025.0.0.0（Nacos 3.0.3 / Sentinel 1.8.9 / Seata 2.5.0） |
| MyBatis-Plus | 3.5.17（boot3 starter + jsqlparser） |
| MySQL | Server 8.4 LTS / 驱动 9.7.0 |
| 接口文档 | springdoc-openapi 2.8.17 + knife4j-openapi3-ui 4.5.0（`/doc.html`、`/swagger-ui.html`） |
| JWT | JJWT 0.13.0（HS256） |

## 环境要求

- JDK 21（`JAVA_HOME` 指向 JDK 21）
- Maven 3.8+
- Docker Desktop（MySQL / Nacos / Sentinel / Seata）

## 快速启动

1. 启动基础设施：

```bash
docker compose up -d
```

等待 Nacos 健康（控制台 `http://localhost:8858`，账号密码 `nacos` / `nacos`；Nacos 3.x 的 8848 是服务端口，
控制台在独立端口 8080，宿主机映射为 8858）。MySQL 会自动执行 [`sql/init.sql`](sql/init.sql)。

2. 在 Nacos 配置管理里导入 [`docs/nacos/`](docs/nacos/) 下的共享配置，Data ID 与格式：

| Data ID | 格式 |
|---------|------|
| `shared-jdbc.yaml` | YAML |
| `shared-log.yaml` | YAML |
| `shared-seata.yaml` | YAML |
| `shared-swagger.yaml` | YAML（可选） |
| `gateway-routes.json` | JSON（可选，启用网关动态路由时用） |

Group 使用 `DEFAULT_GROUP`。**不导入也能启动**：各服务的 `application.yaml` 里已经有环境变量驱动的
数据源与 MyBatis-Plus 默认配置，导入 Nacos 的 `shared-jdbc.yaml` 只是把它换成集中式管理（需在各服务的
`spring.config.import` 里打开对应那一行）。

3. 编译：

```bash
mvn -DskipTests package
```

4. 逐个启动（建议顺序：item → cart → user → trade → pay → gateway）：

```bash
mvn -pl item-service spring-boot:run
```

或在 IDE 中运行各模块的 `*Application`。对外入口统一是 `http://localhost:8080`。

## 接口速查

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/users/login` | 登录，返回 JWT（免登录） |
| GET | `/users/me` | 当前用户信息 |
| GET | `/items/{id}` | 商品详情（免登录） |
| GET | `/items/page?pageNo=1&pageSize=10` | 商品分页 |
| GET | `/search?name=phone` | 搜索（免登录） |
| GET/POST/DELETE | `/carts` | 购物车 |
| POST | `/orders` | 下单 |
| GET | `/orders/{orderId}` | 订单详情 |
| POST | `/pay-orders` | 支付（余额） |
| GET | `/pay-orders/{id}` | 支付单详情 |

登录示例：

```http
POST http://localhost:8080/users/login
Content-Type: application/json

{"username":"demo","password":"123456"}
```

后续请求带上 `Authorization: Bearer <token>`（网关也兼容 `token` 头和 `?token=`）。
各服务接口文档：`http://localhost:<port>/doc.html`。

演示账号：`demo` / `123456`，余额 1000000 分。

## 环境变量

| 变量 | 默认值 | 用途 |
|------|--------|------|
| `NACOS_ADDR` | `127.0.0.1:8848` | Nacos 地址 |
| `NACOS_USERNAME` / `NACOS_PASSWORD` | `nacos` / `nacos` | Nacos 3.x 鉴权 |
| `MYSQL_HOST` / `MYSQL_PORT` | `localhost` / `3306` | MySQL 地址 |
| `MYSQL_USER` / `MYSQL_PASSWORD` | `root` / `123` | MySQL 账号 |
| `SENTINEL_DASHBOARD` | `127.0.0.1:8090` | Sentinel 控制台 |
| `SEATA_ADDR` | `127.0.0.1:8091` | Seata Server |
| `BLUECRYSTAL_JWT_SECRET` | 开发默认值 | JWT 签名密钥（UTF-8 ≥ 32 字节，生产必改） |
| `BLUECRYSTAL_JWT_TTL` | `30m` | token 有效期 |
| `SPRING_PROFILES_ACTIVE` | `dev` | Spring profile |

复制 [`.env.example`](.env.example) 为 `.env` 后按需覆盖。

## 骨架能力边界

- **已实现**：统一的响应体/异常/分页/用户上下文、网关 JWT 鉴权与路由、各服务的分层骨架
  （controller / service / mapper / po / vo）、OpenFeign 客户端与用户、XID 透传、分页插件、
  Hikari 数据源、Docker Compose 基础设施、初始化 SQL。
- **未实现（留了扩展点）**：支付只有余额渠道（`PayChannel` 已预留支付宝/微信）、搜索走数据库 like
  而不是 Elasticsearch、购物车未接 Redis、网关路由是静态的（动态路由配置已备好）、
  没有单元测试、没有消息队列。
- 接口文档与网关白名单在开发期对所有路径开放，上线前请收紧。
