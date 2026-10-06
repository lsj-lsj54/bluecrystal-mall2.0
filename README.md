# 蓝水晶商城 v2（bluecrystal-mall）

基于 **Spring Boot 3.5 + Spring Cloud 2025.0 + Spring Cloud Alibaba 2025.0** 的商城微服务项目：
网关统一鉴权、商品、购物车、用户、订单、余额支付，JDK 21。

> 从旧版（Spring Boot 2.7 / Spring Cloud 2021 / JDK 17）重构而来。
> 版本选型与核实依据见 [docs/VERSION-MATRIX.md](docs/VERSION-MATRIX.md)，调用链与鉴权设计见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)。

## 技术栈

| 分类 | 组件 | 版本 | 说明 |
|------|------|------|------|
| 语言/运行时 | JDK | **21 (LTS)** | `maven.compiler.release=21` |
| 基础框架 | Spring Boot | **3.5.16** | Spring Framework 6.2.19、Tomcat 10.1.55 |
| 微服务 | Spring Cloud | **2025.0.3** | 模块版本 4.3.x（commons 4.3.3 / gateway 4.3.5） |
| 微服务 | Spring Cloud Alibaba | **2025.0.0.0** | Nacos client 3.0.3、Sentinel 1.8.9、Seata 2.5.0 |
| 网关 | Spring Cloud Gateway | **4.3.5（WebFlux）** | `spring-cloud-starter-gateway-server-webflux`，配置前缀 `spring.cloud.gateway.server.webflux.*` |
| 注册中心 | Nacos | **client 3.0.3 / server v3.0.3** | **只做服务发现**，配置全部写在本地 `application.yaml` |
| 配置 | Spring Boot 本地配置 | — | 不使用 Nacos 配置中心，无 `bootstrap.yaml` |
| 持久层 | MyBatis-Plus | **3.5.17** | `mybatis-plus-spring-boot3-starter` + `mybatis-plus-jsqlparser`（分页必需） |
| 数据库 | MySQL | **Server 8.4 LTS / 驱动 9.7.0** | 驱动坐标 `com.mysql:mysql-connector-j` |
| 连接池 | HikariCP | 6.3.3（Boot 管理） | |
| 缓存 | Redis / Lettuce | **Boot 管理（lettuce 6.6.0）** | `spring-boot-starter-data-redis`，用于商品缓存 |
| 分布式事务 | Seata | **2.5.0（`org.apache.seata`）** | AT 模式，`@GlobalTransactional` |
| 流量防护 | Sentinel | **1.8.9** | 服务端 dashboard 1.8.9 |
| 服务调用 | OpenFeign + LoadBalancer | Cloud 2025.0.3 管理 | 自定义 `ErrorDecoder` 保留下游业务错误码 |
| 服务间鉴权传递 | JWT | **JJWT 0.13.0（HS256）** | 网关验签，user-service 签发 |
| 参数校验 | Jakarta Validation | Boot 管理 | `@Valid` + 全局异常处理 |
| 接口文档 | springdoc + knife4j UI | **springdoc 2.8.17 + knife4j-openapi3-ui 4.5.0** | `/v3/api-docs`、`/swagger-ui.html`、`/doc.html` |
| 密码摘要 | Spring Security Crypto | Boot 管理 | `BCryptPasswordEncoder` |
| 工具 | Lombok | 1.18.46（Boot 管理） | 显式配置注解处理器，兼容未来 JDK |
| 构建 | Maven / fabric8 docker 插件 | Maven 3.8+ / **已移除 docker 插件** | 需要镜像时用 `mvn spring-boot:build-image` |
| 监控 | Spring Boot Actuator | Boot 管理 | `/actuator/health`、`/actuator/info` |

## 模块与端口

| 模块 | 端口 | 数据库 | 说明 |
|------|------|--------|------|
| `bluecrystal-common` | — | — | 统一响应体 `R`、异常体系、分页、`UserContext`、自动装配 |
| `bluecrystal-api` | — | — | OpenFeign 客户端、跨服务 DTO、Feign 错误解码器 |
| `bluecrystal-gateway` | 8080 | — | 统一入口、JWT 鉴权、路由转发、内部接口拦截 |
| `item-service` | 8081 | `bc-item` | 商品查询（Redis 缓存）、搜索、扣减库存 |
| `cart-service` | 8082 | `bc-cart` | 购物车增删改查 |
| `user-service` | 8084 | `bc-user` | 登录签发 JWT、用户信息、扣余额（内部接口） |
| `trade-service` | 8085 | `bc-trade` | 下单（Seata 全局事务）、订单查询 |
| `pay-service` | 8086 | `bc-pay` | 支付单、余额支付 |

## 环境要求

- JDK 21（`JAVA_HOME` 指向 JDK 21）
- Maven 3.8+
- MySQL 8.x（必须）
- Redis 7.x（可选：Redis 挂了 item-service 会自动降级为直接查库）
- Nacos 3.x（可选：只影响服务发现，本地单机调试可直接访问各服务端口）
- Docker Desktop（可选，用于一键起基础设施）

## 快速启动

1. 启动基础设施（有 Docker 时）：

```bash
docker compose up -d
```

会拉起 MySQL 8.4、Redis 7.4、Nacos v3.0.3、Sentinel 1.8.9、Seata 2.5.0。
MySQL 首次启动会自动执行 [`sql/init.sql`](sql/init.sql)（建 5 个 `bc-*` 库、Seata `undo_log`、演示数据）。
Nacos 控制台：`http://localhost:8858`（Nacos 3.x 控制台是独立端口，账号密码 `nacos` / `nacos`）。

> **不需要在 Nacos 里配置任何东西。** 所有配置都在各模块的 `application.yaml` 里。

2. 编译：

```bash
mvn -DskipTests package
```

3. 启动服务（建议顺序：item → cart → user → trade → pay → gateway）：

```bash
mvn -pl item-service spring-boot:run
```

或在 IDE 里运行各模块的 `*Application`。对外统一入口是 `http://localhost:8080`。

## 配置说明

- **所有配置都在 `application.yaml`**：端口、数据源、Redis、MyBatis-Plus、Seata、Sentinel、日志、
  接口文档、JWT 密钥，没有任何外部配置源（无 `bootstrap.yaml`、无 Nacos 配置中心）。
- 配置里的 `${XXX:默认值}` 可以用**环境变量或命令行参数覆盖**，例如：

```bash
java -jar item-service/target/item-service.jar \
  --spring.datasource.password=你的密码 \
  --spring.data.redis.host=127.0.0.1
```

- Nacos 仅作为注册中心，`spring.cloud.nacos.discovery.server-addr` 默认 `127.0.0.1:8848`；
  没起 Nacos 时服务仍能启动（只是注册失败，网关无法按服务名转发）。

## 接口速查

| 方法 | 路径 | 说明 | 是否需要登录 |
|------|------|------|--------------|
| POST | `/users/login` | 登录，返回 JWT | 否 |
| GET | `/users/me` | 当前用户信息 | 是 |
| GET | `/items/{id}` | 商品详情（走 Redis 缓存） | 否 |
| GET | `/items?ids=1,2` | 批量查询商品 | 否 |
| GET | `/items/page?pageNo=1&pageSize=10` | 商品分页 | 否 |
| GET | `/search?name=phone` | 搜索商品 | 否 |
| GET | `/carts` | 我的购物车 | 是 |
| POST | `/carts` | 加入购物车 | 是 |
| DELETE | `/carts?itemIds=1,2` | 移除购物车商品 | 是 |
| POST | `/orders` | 下单（Seata 全局事务） | 是 |
| GET | `/orders/{orderId}` | 订单详情 | 是 |
| POST | `/pay-orders` | 发起余额支付 | 是 |
| GET | `/pay-orders/{id}` | 支付单详情 | 是 |

**内部接口（不对外暴露）**：`/internal/**` 是服务间调用专用，网关对该前缀直接返回 403，
外部无法调用。包括扣余额 `POST /internal/users/{userId}/balance/deduct`、
核对订单 `GET /internal/orders/{orderId}`、回写支付 `PUT /internal/orders/{orderId}/pay-success`。

登录示例：

```http
POST http://localhost:8080/users/login
Content-Type: application/json

{"username":"demo","password":"123456"}
```

后续请求携带 `Authorization: Bearer <token>`（也兼容 `token` 请求头和 `?token=`）。
接口文档：`http://localhost:<port>/doc.html` 或 `/swagger-ui.html`。

演示账号：`demo` / `123456`，余额 1000000 分。

## 环境变量

| 变量 | 默认值 | 用途 |
|------|--------|------|
| `NACOS_ADDR` | `127.0.0.1:8848` | Nacos 注册中心地址 |
| `NACOS_USERNAME` / `NACOS_PASSWORD` | `nacos` / `nacos` | Nacos 3.x 鉴权 |
| `MYSQL_HOST` / `MYSQL_PORT` | `localhost` / `3306` | MySQL 地址 |
| `MYSQL_USER` / `MYSQL_PASSWORD` | `root` / `123` | MySQL 账号 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | `localhost` / `6379` / 空 | Redis（item-service 缓存） |
| `SENTINEL_DASHBOARD` | `127.0.0.1:8090` | Sentinel 控制台 |
| `SEATA_ADDR` | `127.0.0.1:8091` | Seata Server |
| `SEATA_ENABLED` | `true` | 关掉全局事务（如 `false`）便于本地调试 |
| `BLUECRYSTAL_JWT_SECRET` | 开发默认值 | JWT 签名密钥，UTF-8 需 ≥ 32 字节，生产必改 |
| `BLUECRYSTAL_JWT_TTL` | `30m` | token 有效期 |

## 关键设计（与安全/正确性相关）

1. **网关剥离伪造身份**：客户端自带的 `X-User-Id` 一律先删除，只信任网关验签后写入的值。
2. **内部接口不对外**：`/internal/**` 在网关被 `SetStatus=403` 挡掉，服务本身还会二次校验归属，
   避免「任何登录用户都能扣别人余额」「不付钱就把自己订单标记成已支付」。
3. **服务端定价**：下单金额由 trade-service 回查商品服务实时价格计算，**不信任前端传的 price**。
4. **支付前核对订单**：pay-service 先向 trade-service 核对订单存在、归属、待支付状态、金额一致，
   全部通过才扣款；金额不一致直接拒绝。
5. **Feign 错误透传**：自定义 `ErrorDecoder` 把下游统一响应体还原成业务异常，
   「库存不足」「余额不足」等真实原因不会退化成「服务器繁忙」。
6. **缓存一致性**：扣库存后立即删除商品缓存；Redis 不可用时自动降级为直接查库，不影响可用性。
7. **异常语义正确**：404/405/415 不会被兜底成 500，便于排查。

## 能力边界

- 支付只实现余额渠道（`PayChannel` 已预留支付宝/微信）。
- 搜索走数据库 `like`，未接 Elasticsearch。
- 网关路由是静态配置，未接动态路由。
- 没有单元测试；消息队列未使用（订单超时关闭等留了 TODO）。
- 接口文档与网关白名单在开发期较宽松，上线前需收紧。
