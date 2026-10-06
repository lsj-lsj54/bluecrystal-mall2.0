# 蓝水晶商城 v2 架构说明

## 1. 模块与端口

| 模块 | 端口 | 数据库 | 说明 |
|------|------|--------|------|
| `bluecrystal-common` | — | — | 统一响应体、异常、分页、用户上下文、自动装配 |
| `bluecrystal-api` | — | — | OpenFeign 客户端与跨服务 DTO |
| `bluecrystal-gateway` | 8080 | — | 统一入口、JWT 鉴权、路由（WebFlux） |
| `item-service` | 8081 | `bc-item` | 商品查询、搜索、扣减库存 |
| `cart-service` | 8082 | `bc-cart` | 购物车增删改查 |
| `user-service` | 8084 | `bc-user` | 登录签发 JWT、用户信息、扣余额 |
| `trade-service` | 8085 | `bc-trade` | 下单（Seata 全局事务） |
| `pay-service` | 8086 | `bc-pay` | 支付单（骨架仅余额支付） |

## 2. 调用链

```
客户端 → gateway:8080 ──(lb://, Nacos 服务发现)──┬→ item-service
                                                ├→ cart-service ──Feign──→ item-service（补全商品信息）
                                                ├→ user-service
                                                ├→ trade-service ─Feign─→ item-service（扣库存）
                                                │                 └Feign─→ cart-service（清购物车）
                                                └→ pay-service ──Feign──→ user-service（扣余额）
                                                                └Feign──→ trade-service（标记已支付）
```

下单 `POST /orders` 与支付 `POST /pay-orders` 都标注了 `@GlobalTransactional`，库存、订单、购物车、余额
任一步失败都会整体回滚（Seata AT 模式，各库需有 `undo_log` 表，见 `sql/init.sql`）。

## 3. 鉴权与用户上下文

1. `user-service` 校验 BCrypt 密码，用 JJWT 签发 token（claim `user` = 用户 id，HS256 共享密钥）。
2. 网关 `AuthGlobalFilter`（`Ordered.HIGHEST_PRECEDENCE + 100`）：
   - **先删除**客户端自带的 `X-User-Id`，防止伪造身份直连下游；
   - 命中 `bc.auth.exclude-paths` 白名单的请求直接放行；
   - 其余请求验签失败即返回 `401` + JSON 响应体；
   - 验签成功后写入 `X-User-Id` 再转发。
3. 下游 MVC 服务的 `UserInfoInterceptor` 把 `X-User-Id` 放进 `UserContext`（ThreadLocal），请求结束清理。
4. `DefaultFeignConfig` 把 `X-User-Id` 继续透传给下游；`SeataFeignConfig` 额外透传 `TX_XID`。

> 骨架使用 HS256 共享密钥，网关与 `user-service` 必须配置同一个 `bc.jwt.secret`。
> 生产环境建议换成 RS256：网关只持有公钥，`user-service` 持有私钥。

## 4. 配置分层

| 位置 | 内容 |
|------|------|
| 各服务 `application.yaml` | 端口、服务名、库名（`bc.db.database`）、**环境变量驱动的数据源默认值**、Nacos/Sentinel 地址、接口文档开关 |
| `docs/nacos/shared-jdbc.yaml` | （可选）集中式数据源（Hikari）与 MyBatis-Plus 全局约定；默认已写在各服务本地，需要集中管理时在 `spring.config.import` 中打开 |
| `docs/nacos/shared-log.yaml` | 日志级别与格式 |
| `docs/nacos/shared-seata.yaml` | Seata 客户端（事务分组、注册中心） |
| `docs/nacos/shared-swagger.yaml` | 接口文档集中配置（可选，默认写在各服务本地） |
| `docs/nacos/gateway-routes.json` | 网关动态路由 JSON（骨架默认用 `application.yaml` 里的静态路由） |
| `.env` / 环境变量 | `NACOS_ADDR`、`MYSQL_*`、`BLUECRYSTAL_JWT_SECRET` 等 |

配置引入方式统一为：

```yaml
spring:
  config:
    import:
      - optional:nacos:shared-log.yaml
      - optional:nacos:shared-seata.yaml
```

`optional:` 前缀 + 本地默认值，意味着**只起 MySQL（甚至什么都不起）也能把服务拉起来**：
没有 Nacos 时走本地默认配置，接入 Nacos 后再由共享配置接管。业务服务要真正读写数据仍需可用的 MySQL。

## 5. 扩展点

- **网关动态路由**：`docs/nacos/gateway-routes.json` 已备好，接一个 `RouteDefinitionRepository`
  读取 Nacos 配置即可替换静态路由。
- **服务容错**：各服务已引入 Sentinel starter，在 `@FeignClient` 上加 `fallbackFactory` 并打开
  `feign.sentinel.enabled=true` 就能降级。
- **缓存**：`cart-service`、`item-service` 里的 `// TODO` 标出了接 Redis 的位置。
- **支付渠道**：`PayChannel` 枚举已预留 `alipay` / `wx`，`PayOrderServiceImpl` 标出了扩展位置。
- **接口文档**：访问 `http://localhost:<port>/doc.html`。
