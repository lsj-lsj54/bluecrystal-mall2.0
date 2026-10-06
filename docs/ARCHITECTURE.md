# 蓝水晶商城 v2 架构说明

## 1. 模块与端口

| 模块 | 端口 | 数据库 | 说明 |
|------|------|--------|------|
| `bluecrystal-common` | — | — | 统一响应体 `R`、异常体系、分页、用户上下文、自动装配 |
| `bluecrystal-api` | — | — | OpenFeign 客户端、跨服务 DTO、Feign 错误解码器 |
| `bluecrystal-gateway` | 8080 | — | 统一入口、JWT 鉴权、路由（WebFlux）、内部接口拦截 |
| `item-service` | 8081 | `bc-item` | 商品查询（Redis 缓存）、搜索、扣减库存 |
| `cart-service` | 8082 | `bc-cart` | 购物车增删改查 |
| `user-service` | 8084 | `bc-user` | 登录签发 JWT、用户信息、扣余额（内部接口） |
| `trade-service` | 8085 | `bc-trade` | 下单（Seata 全局事务）、订单查询 |
| `pay-service` | 8086 | `bc-pay` | 支付单、余额支付 |

## 2. 调用链

```
客户端 → gateway:8080 ──(lb://, Nacos 服务发现)──┬→ item-service
                                                ├→ cart-service ──Feign──→ item-service（补全商品信息）
                                                ├→ user-service
                                                ├→ trade-service ─Feign─→ item-service（查价 + 扣库存）
                                                │                 └Feign─→ cart-service（清购物车）
                                                └→ pay-service ──Feign──→ trade-service（核对订单 / 回写已支付）
                                                                └Feign──→ user-service（扣余额）

内部接口（服务间 Feign 直连，不经过网关）：
  trade-service → item-service   POST /items/stock/deduct
  trade-service → cart-service   DELETE /carts?itemIds=
  pay-service   → trade-service  GET  /internal/orders/{id}、PUT /internal/orders/{id}/pay-success
  pay-service   → user-service   POST /internal/users/{userId}/balance/deduct
```

下单 `POST /orders` 与支付 `POST /pay-orders` 都标注了 `@GlobalTransactional`，库存、订单、购物车、余额
任一步失败都会整体回滚（Seata AT 模式，各库需有 `undo_log` 表，见 `sql/init.sql`）。

## 3. 鉴权与用户上下文

1. `user-service` 校验 BCrypt 密码，用 JJWT 签发 token（claim `user` = 用户 id，HS256 共享密钥）。
2. 网关 `AuthGlobalFilter`（`Ordered.HIGHEST_PRECEDENCE + 100`）：
   - **先删除**客户端自带的 `X-User-Id`，防止伪造身份直连下游；
   - 命中 `bc.auth.exclude-paths` 白名单（`/items/**`、`/search/**`、`/users/login`、`/actuator/health`）直接放行；
   - 其余请求验签失败即返回 `401` + JSON 响应体；
   - 验签成功后写入 `X-User-Id` 再转发。
3. 下游 MVC 服务的 `UserInfoInterceptor` 把 `X-User-Id` 放进 `UserContext`（ThreadLocal），请求结束清理。
4. `DefaultFeignConfig` 把 `X-User-Id` 继续透传给下游；`SeataFeignConfig` 额外透传 `TX_XID`。

> HS256 共享密钥：网关与 `user-service` 必须配置同一个 `bc.jwt.secret`。生产建议改 RS256（网关只持公钥）。

### 内部接口为什么不能对外暴露

`/internal/**` 是服务间调用专用的**敏感写操作**（扣余额、回写订单支付状态）。如果直接经网关暴露，
任何登录用户都能：

- 传 `userId` 扣**别人**的余额；
- 不付钱就调 `PUT /orders/{id}/pay-success` 把自己的订单改成已支付。

因此做了三层防护：

1. **网关**：`internal-deny` 路由把 `/internal/**` 直接 `SetStatus=403`，外部访问不到；
2. **服务层归属校验**：user-service 校验「被扣款人 == 当前登录用户」，trade-service 校验「订单属于当前登录用户」；
3. **Feign 路径**：`UserClient`/`TradeClient` 的 `path` 就指向 `/internal/**`，服务间调用走服务名直连，不经过网关。

## 4. 配置

**不使用 Nacos 配置中心**：所有配置都写在各模块自己的 `application.yaml` 里，没有 `bootstrap.yaml`、
没有 `spring.config.import`，Nacos 只承担服务发现。

| 配置项 | 位置 |
|--------|------|
| 端口、服务名、库名（`bc.db.database`）、数据源（Hikari） | 各服务 `application.yaml`，支持 `${MYSQL_HOST:localhost}` 这类环境变量覆盖 |
| Redis（仅 item-service） | `item-service/application.yaml` 的 `spring.data.redis.*` |
| MyBatis-Plus 全局约定 | 各服务 `application.yaml` 的 `mybatis-plus.*` |
| Seata 客户端（事务分组、file 注册中心） | 各服务 `application.yaml` 的 `seata.*`，可用 `SEATA_ENABLED=false` 关闭 |
| Sentinel 控制台 | 各服务 `application.yaml` 的 `spring.cloud.sentinel.*` |
| 日志级别与格式 | 各服务 `application.yaml` 的 `logging.*` |
| JWT 密钥与白名单 | `bc.jwt.*`（gateway + user-service 保持一致）、`bc.auth.exclude-paths`（gateway） |
| 路由 | `bluecrystal-gateway/application.yaml` 的 `spring.cloud.gateway.server.webflux.routes` |
| 接口文档 | 各服务 `application.yaml` 的 `springdoc.*` |

带默认值的占位符使服务**不依赖任何外部配置源**也能启动：只起 MySQL 就能跑通除缓存外的全部功能。

## 5. 缓存设计（item-service）

- 缓存 key：`item:{id}`，value 为商品对象，TTL 60 分钟。
- **防缓存穿透**：数据库也查不到时写入一个「空值占位」（id 为 null 的 Item），TTL 60 秒，
  读取时识别占位并直接抛「商品不存在」，避免同一个不存在的 id 反复打库。
- **防脏读**：扣减库存成功后立即删除对应 key，库存变化不会被旧缓存掩盖。
- **批量优化**：`queryByIds` 用一次 `multiGet` 取回全部 key，避免 N+1 次网络往返。
- **序列化**：`RedisConfig` 显式构造 ObjectMapper（注册 `JavaTimeModule` + 打开默认类型信息），
  否则含 `LocalDateTime` 的实体会在写入缓存时报
  `InvalidDefinitionException: Java 8 date/time type ... not supported by default`。
- **故障降级**：缓存读写包了 `DataAccessException` 兜底，Redis 挂掉时记 warn 并直接查库，
  不会因为缓存不可用导致商品接口整体不可用。

## 6. 金额与状态的一致性约束

- **下单不信任前端价格**：`trade-service` 先 `GET /items?ids=` 回查商品，用**服务端价格**生成订单明细快照
  并计算总价，前端传上来的 price 被忽略。
- **支付前先核对订单**：`pay-service` 调 `GET /internal/orders/{id}`，校验订单存在、归属当前用户、
  状态为待支付、金额与本次支付一致，四项全过才扣款；金额不一致直接拒绝。
- **Feign 错误透传**：`BluecrystalFeignErrorDecoder` 把下游统一响应体 `R` 还原成业务异常，
  所以「库存不足」「余额不足」能带着原始 code/msg 传到最外层，而不是变成 500「服务器繁忙」。

## 7. 扩展点

- **网关动态路由**：目前是 `application.yaml` 里的静态路由；接入 `RouteDefinitionRepository` 即可改为动态。
- **服务容错**：各服务已引入 Sentinel starter，在 `@FeignClient` 上加 `fallbackFactory` 并打开
  `feign.sentinel.enabled=true` 就能降级。
- **缓存**：`cart-service` 的 `// TODO` 标出了接 Redis 的位置。
- **支付渠道**：`PayChannel` 枚举已预留 `alipay` / `wx`，`PayOrderServiceImpl` 标出了扩展位置。
- **接口文档**：访问 `http://localhost:<port>/doc.html`（knife4j UI）或 `/swagger-ui.html`（springdoc）。
