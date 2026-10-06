# 技术栈版本矩阵（v1 → v2）

本文件记录重构后的目标版本，以及每个版本的**核实依据**。所有版本号均于本机通过 Maven Central 的
`maven-metadata.xml` / 实际 pom 文件核对过，不是凭记忆填写的。

## 1. 目标矩阵

| 组件 | v1（旧） | v2（本次目标） | 说明 |
|------|----------|----------------|------|
| JDK | 17 | **21 (LTS)** | 本机 `JAVA_HOME` 已是 JDK 21.0.12；`maven.compiler.release=21` |
| Spring Boot | 2.7.12（已 EOL） | **3.5.16** | 3.5.x 线最新；3.5.16 管理 Spring Framework 6.2.19、Tomcat 10.1.55、Lombok 1.18.46 |
| Spring Cloud | 2021.0.3 | **2025.0.3** | `spring-cloud-starter-parent:2025.0.3` 的 parent 就是 `spring-boot-starter-parent:3.5.15`，即 2025.0.x ↔ Boot 3.5.x；该训练线的**模块版本是 4.3.x**（实测 `spring-cloud-commons 4.3.3`、gateway `4.3.5`） |
| Spring Cloud Alibaba | 2021.0.4.0 | **2025.0.0.0** | 该 BOM 管理 `nacos-client 3.0.3`、`sentinel 1.8.9`、`seata 2.5.0` |
| 网关 | `spring-cloud-starter-gateway` | **`spring-cloud-starter-gateway-server-webflux` 4.3.5** | artifact 已更名（4.3 起）；配置前缀改为 `spring.cloud.gateway.server.webflux.*`。注意 5.0.x 属于 Spring Cloud 2025.1.x（Boot 4 线），不要混用 |
| MyBatis-Plus | 3.4.2 | **3.5.17** | 用 `mybatis-plus-spring-boot3-starter`（该 starter 自身基于 Boot 3.5.9 构建）；3.5.9+ 起分页需额外引入 `mybatis-plus-jsqlparser` |
| MySQL 驱动 | `mysql:mysql-connector-java` 8.0.23 | **`com.mysql:mysql-connector-j`（Boot 管理 9.7.0）** | 坐标迁移，不再手写版本 |
| MySQL Server | 8.0 | **8.4 LTS** | 8.4 起移除 `--default-authentication-plugin=mysql_native_password` |
| Seata | `io.seata` 1.7.1 | **`org.apache.seata` 2.5.0** | 坐标迁移到 Apache；服务端控制台端口 7091 |
| Nacos | server 2.2.3 / client 2.x | **client 3.0.3 + server v3.0.3** | Nacos 3.x 控制台改独立端口 8080（compose 映射到宿主机 8858） |
| Sentinel | dashboard 1.8.6 | **1.8.9** | 与 `sentinel-core 1.8.9` 对齐 |
| 接口文档 | knife4j openapi2 4.1.0（swagger 1.6，javax） | **springdoc-openapi 2.8.17 + knife4j-openapi3-ui 4.5.0** | ⚠️ 实测：knife4j 的 `-jakarta-spring-boot-starter` 内置 springdoc 2.3.0，在 Boot 3.5 下会抛 `NoSuchMethodError: SpringDocConfigProperties.getGroupConfigs()`，`/v3/api-docs` 直接 500；因此只保留 knife4j 的**静态增强 UI**（`/doc.html`），文档能力由 springdoc 2.8.17 提供 |
| 密码校验 | `spring-security-rsa` 1.0.10.RELEASE（javax） | **`spring-security-crypto`（Boot 管理）** | 用 `BCryptPasswordEncoder` |
| JWT | Hutool JWT + JKS/RS256 + keytool 脚本 | **JJWT 0.13.0，HS256 共享密钥** | Boot 3.5 的 BOM 不再管理 nimbus/jjwt，故在根 POM 显式声明 |
| Hutool | 5.8.11 | **移除** | 骨架不再依赖；需要时加 `cn.hutool:hutool-all:5.8.47` |
| Redis 客户端 | （v1 只声明了 Caffeine，未真正用缓存） | **`spring-boot-starter-data-redis`（Boot 管理 lettuce 6.6.0）** | v2 商品缓存使用 Redis；**不要**再单独 pin `io.lettuce:lettuce-core`（曾写成 7.5.2.RELEASE，绕过 Boot 兼容矩阵） |
| Caffeine / RabbitMQ | 声明但业务未用 | **移除** | 消除「声明了却没人用」的依赖 |
| 容器化 | fabric8 docker-maven-plugin 0.42.1 | **移除，改用 `mvn spring-boot:build-image`** | 旧插件版本过老且绑定 package 阶段拖慢构建 |
| 配置加载 | `bootstrap.yaml` + `spring-cloud-starter-bootstrap` | **`spring.config.import: optional:nacos:xxx.yaml`** | Boot 2.4+ 起的官方方式；`optional:` 保证本地无 Nacos 也能启动 |

## 2. 核实依据（Maven Central）

| 结论 | 依据 |
|------|------|
| Boot 3.5.x 最新为 3.5.16；Boot 4.0.x 最新为 4.0.8 | `org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml` |
| Spring Cloud 2025.0.3 对应 Boot 3.5.15 | `org/springframework/cloud/spring-cloud-starter-parent/2025.0.3/...pom` 的 `<parent>` |
| Spring Cloud 2025.0.x 的模块是 4.3.x（不是 5.0.x） | 构建产物 `bluecrystal-gateway.jar` 的 BOOT-INF/lib 实测：`spring-cloud-gateway-server-webflux-4.3.5.jar`、`spring-cloud-commons-4.3.3.jar`；5.0.x 出现在 `spring-cloud-dependencies/2025.1.3` |
| Spring Cloud 2025.1.x 对应 Boot 4.0.x | `spring-cloud-starter-parent/2025.1.0-RC1` 的 parent 是 `spring-boot-starter-parent:4.0.0-RC2` |
| SCA 2025.0.0.0 → nacos-client 3.0.3 / sentinel 1.8.9 / seata 2.5.0（`org.apache.seata`） | `com/alibaba/cloud/spring-cloud-alibaba-dependencies/2025.0.0.0/...pom` |
| SCA 2025.0.0.0 的 parent 是 `spring-cloud-dependencies-parent:4.3.0`（即 Spring Cloud 2025.0.x 线） | 同上 pom 的 `<parent>` |
| MyBatis-Plus 3.5.17 的 boot3 starter 基于 Boot 3.5.9 | `com/baomidou/mybatis-plus-spring-boot3-starter/3.5.17/...pom` |
| knife4j 4.5.0 内置 springdoc 2.3.0，与 Boot 3.5 不兼容 | `com/github/xiaoymin/knife4j/4.5.0/knife4j-4.5.0.pom` 的 `knife4j-springdoc-openapi-jakarta.version`；本机启动实测报 `NoSuchMethodError: SpringDocConfigProperties.getGroupConfigs()` |
| springdoc 2.8.17 的 parent 是 Boot 3.5.13 | `org/springdoc/springdoc-openapi/2.8.17/...pom` 的 `<parent>` |
| Boot 3.5.16 管理 mysql 9.7.0 / lombok 1.18.46 | `org/springframework/boot/spring-boot-dependencies/3.5.16/...pom` 的 properties |

> **未能核实的部分**：Docker Hub 在本机被 hosts 屏蔽（`hub.docker.com` 解析到 127.0.0.1），因此
> `docker-compose.yml` 里的镜像 tag（`nacos/nacos-server:v3.0.3`、`seataio/seata-server:2.5.0`、
> `bladex/sentinel-dashboard:1.8.9`）无法联网确认，已在 compose 里写了备选 tag 注释。

## 3. Boot 2.7 → 3.5 的主要破坏性变更（本次已处理）

1. **javax → jakarta**：`javax.servlet.*` 全部换成 `jakarta.servlet.*`（拦截器、全局异常处理）。
2. **自动装配注册文件变更**：`META-INF/spring.factories` → `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，配置类改用 `@AutoConfiguration`。
3. **移除 `spring.mvc.pathmatch.matching-strategy: ant_path_matcher`**：Boot 3 只保留 PathPatternParser；网关侧的白名单匹配改用 `PathPatternParser`。
4. **彻底不用 Nacos 配置中心**：Boot 2.4+ 的 `spring.config.import` 也不再使用 —— 所有配置直接写在各模块
   `application.yaml` 里，Nacos 只保留服务发现（`spring-cloud-starter-alibaba-nacos-config` 与
   `docs/nacos/*.yaml` 已删除）。
5. **网关配置前缀变更**：`spring.cloud.gateway.routes` → `spring.cloud.gateway.server.webflux.routes`。
6. **MyBatis-Plus 分页**：需显式引入 `mybatis-plus-jsqlparser`，否则 `PaginationInnerInterceptor` 不生效/报错。
7. **Swagger 注解**：`io.swagger.annotations.*`（OpenAPI 2）→ `io.swagger.v3.oas.annotations.*`（OpenAPI 3）。
8. **Seata 坐标与配置**：`io.seata` → `org.apache.seata`，客户端包名 `org.apache.seata.core.context.RootContext`。

## 4. 备选方案：升级到 Boot 4 / Spring Cloud 2025.1

若希望再往前一代，只改根 POM 的几个属性即可，但要注意生态差异：

```xml
<spring-boot.version>4.0.8</spring-boot.version>              <!-- Boot 4.0.x GA -->
<spring-cloud.version>2025.1.3</spring-cloud.version>          <!-- 2025.1.x ↔ Boot 4.0.x -->
<spring-cloud-alibaba.version>2025.1.0.0</spring-cloud-alibaba.version>
```

- MyBatis-Plus：改用 `mybatis-plus-spring-boot4-starter`（3.5.17 已提供该 artifact）。
- 接口文档：knife4j 4.5.0 的 starter 连 Boot 3.5 都不兼容（见上表），Boot 4 下更应直接改用
  `springdoc-openapi-starter-webmvc-ui 3.x`（3.1.1 已发布）并放弃 knife4j 的增强 UI。
- Maven 编译插件版本要跟上 Boot 4 的 `spring-boot-starter-parent`。

## 5. 本机实测验证结果

| 验证项 | 命令 / 方式 | 结果 |
|--------|-------------|------|
| 全量编译打包 | `mvn -B -DskipTests package` | ✅ 9 个模块全部 `BUILD SUCCESS` |
| 网关启动 | `java -jar bluecrystal-gateway.jar` | ✅ Boot 3.5.16 + Netty 起在 8080，路由加载正常 |
| 网关健康检查 | `GET /actuator/health` | ✅ 200 `{"status":"UP"}` |
| 鉴权拦截 | `GET /carts`（无 token） | ✅ 401 + `{"code":401,"msg":"未登录","data":null}` |
| 伪造身份 | `GET /carts` 带 `X-User-Id: 999` | ✅ 仍 401（客户端自带的 `X-User-Id` 被网关剥离） |
| 白名单放行 + 路由命中 | `GET /items/1` | ✅ 503（白名单放行、路由匹配成功，只是没有 item 实例可转发） |
| 服务启动 | `java -jar item-service.jar` | ✅ 起在 8081（common 自动装配、MyBatis-Plus、Sentinel、Seata 客户端全部装配成功） |
| 接口文档 | `GET /v3/api-docs`、`/doc.html` | ✅ 200（含 `/items` 等接口）；`/swagger-ui.html` → 302 跳转 |
| 全局异常处理 | `GET /items/1`（数据库连不上） | ✅ 500 + 统一响应体 `{"code":500,"msg":"服务器繁忙，请稍后再试"}` |
| Seata 客户端 | 启动日志 | ✅ 加载 `org.apache.seata` 2.5.0（日志里同时提示旧包名 `io.seata` 的兼容类被忽略，属正常） |

### 验证过程中发现并修正的问题

1. **knife4j starter 不可用**：最初用 `knife4j-openapi3-jakarta-spring-boot-starter` + 覆盖 springdoc 2.8.17，
   启动后 `/v3/api-docs` 报 `NoSuchMethodError: SpringDocConfigProperties.getGroupConfigs()`。
   已改为 `knife4j-openapi3-ui`（纯静态 UI，保留 `/doc.html`）+ springdoc 2.8.17。
2. **网关 actuator 没有 gateway 端点**：Spring Cloud 2025.0.x 的 gateway 4.3.5 虽然 jar 里有
   `GatewayControllerEndpoint`，但其自动装配并未注册（`AutoConfiguration.imports` 中没有对应的 actuator 配置类），
   因此 `/actuator/gateway/routes` 返回 404。已从 `management.endpoints.web.exposure.include` 中去掉 `gateway`。
3. **服务在没有 Nacos 时起不来**：数据源原本只放在 Nacos 的 `shared-jdbc.yaml` 里，
   导致本地只有 MySQL 时启动失败（`Failed to configure a DataSource`）。已改为
   **各服务 `application.yaml` 内置环境变量驱动的数据源默认值**；随后按要求彻底移除 Nacos 配置中心，
   日志、Seata、Redis 等配置也一并落到各服务本地文件。

## 6. 代码审计：发现并修复的 Bug

| # | 位置 | 问题 | 修复 |
|---|------|------|------|
| 1 | `CommonExceptionAdvice` | 只有 `@ExceptionHandler(Exception.class)` 兜底，Spring MVC 的 404/405/415 全被当成 500，排查时严重误导 | 补 `NoResourceFoundException`(404)、`HttpRequestMethodNotSupportedException`(405)、`HttpMediaTypeNotSupportedException`(415)、缺参/类型不匹配(400) 的专门处理 |
| 2 | `DefaultFeignConfig` | 下游返回 4xx/5xx 时 Feign 只抛 `FeignException`，`R` 里的 code/msg 全丢，最终显示「服务器繁忙」 | 新增 `BluecrystalFeignErrorDecoder`，把响应体解析回 `CommonException` |
| 3 | `ItemStockDeductDTO` + `ItemServiceImpl` | 扣库存入参没有校验，`num` 传负数时 `stock >= num` 恒真 → **扣库存变成加库存** | DTO 加 `@NotNull/@Min(1)`，Controller 用 `List<@Valid ...>` 触发元素校验，服务层再兜一次 |
| 4 | `OrderServiceImpl.createOrder` | 用**前端传来的 price** 计算总价，把 price 改成 1 就能一分钱下单 | 先 `ItemClient` 回查商品，用服务端价格生成快照并计算总价，商品不存在直接拒绝 |
| 5 | `PayOrderServiceImpl.applyPay` | 金额校验是 `form.amount()` 与刚被赋值的 `payOrder.getAmount()` 自比，恒成立；从不核对订单真实金额与归属 | 新增 `GET /internal/orders/{id}`，扣款前核对订单存在/归属/状态/金额一致 |
| 6 | `UserController`、`OrderController` | 内部写接口经网关对外暴露：任何登录用户可扣**别人**余额（传 userId），或不付钱直接 `pay-success` 改自己订单为已支付 | 内部接口迁到 `/internal/**`（网关 `SetStatus=403` 拦截）+ 服务层归属校验 |
| 7 | `RedisConfig` | `GenericJackson2JsonRedisSerializer` 默认 ObjectMapper 未注册 `JavaTimeModule`，缓存含 `LocalDateTime` 的 `Item` 时报 `InvalidDefinitionException` | 显式构造 ObjectMapper（JavaTimeModule + 默认类型信息） |
| 8 | `ItemServiceImpl.queryByIds` | 逐个 `get()`，N 个商品 = N 次 Redis 往返 | 改成一次 `multiGet` |
| 9 | `ItemServiceImpl.deductStock` | 改库存后不删缓存 → 最长 60 分钟读到旧库存 | 扣减成功后删除对应缓存 key |
| 10 | `ItemServiceImpl` 缓存读写 | Redis 不可用时商品接口整体不可用（本机 Redis 默认没起） | 缓存读写包 `DataAccessException` 降级为直接查库 |
| 11 | `item-service/pom.xml` | `bluecrystal-api` 重复声明两次（其中一次硬编码 `2.0.0-SNAPSHOT`）；`io.lettuce:lettuce-core` 被 pin 到 7.5.2.RELEASE，而 Boot 3.5.16 管理的是 6.6.0 | 删除重复依赖与 lettuce 显式版本，统一交给 Boot BOM |

### 尚未能验证的部分（环境限制）

- **数据库读写**：本机 3306 上的 MySQL 拒绝 `root/123`（该密码是本骨架的默认值），所以只验证到
  「Web 层 → MyBatis-Plus → Hikari → MySQL 驱动」这一段（能正确抛出 `Access denied`）。
  换成真实密码后（`MYSQL_PASSWORD=...`）即可跑通 `sql/init.sql` 的数据。
- **Nacos / Sentinel / Seata 服务端联通**：本机 8848 / 8090 / 8091 都没有服务在跑。
- **Docker 镜像 tag**：本机没有 `docker` 命令，且 Docker Hub 被 hosts 屏蔽，`docker-compose.yml` 里的
  镜像 tag 无法联网核实（compose 里已写备选 tag 注释）。
