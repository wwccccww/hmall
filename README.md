# hmall（微服务版）一键启动与演示

本仓库为 Spring Cloud 微服务项目，包含商品、购物车、用户、交易、支付、促销（优惠券秒杀）与网关等模块。

## 1. 快速启动（Docker Compose）

### 1.1 前置

- 已安装 Docker Desktop（Windows/macOS）或 Docker Engine（Linux）
- 已安装 Docker Compose v2（`docker compose`）

### 1.2 启动

在仓库根目录执行：

```bash
docker compose up -d --build
```

首次启动会：

- 拉起 MySQL / Redis / RabbitMQ / Nacos
- 初始化 MySQL 数据库（创建库 + 初始化 `hm-promotion` 的核心表）
- 启动各微服务与网关
- 写入 Nacos 配置：`shared-jdbc.yaml` / `shared-log.yaml` / `shared-swagger.yaml` / `shared-observability.yaml` / `shared-seata.yaml` / `gateway-routes.json`
- **Seata TC** 需在虚拟机部署并注册到 Nacos（服务名 `**seata-server`**，客户端经 `**192.168.116.130:8848`** 发现；TC 业务端口常为 `**8091**`，控制台可能为 `**7091**`）

### 1.3 验证

- **网关**：`http://localhost:8080`
- **促销服务文档**：`http://localhost:8087/doc.html`
- **用户服务文档**：`http://localhost:8084/doc.html`
- **第二个促销实例（可选）**：`http://localhost:8088/doc.html`
- **RabbitMQ 管理台**：`http://localhost:15672`（默认用户 `sail`，密码 `123`，可在 `docker-compose.yml` 通过环境变量覆盖）
- **Seata**：TC 通过 **Nacos** 发现（虚拟机 `**192.168.116.130:8848`**，注册名 `**seata-server`**）；事务组 `**hmall**`，与 `docker/nacos/init/shared-seata.yaml` 中 `tx-service-group` / `vgroup-mapping` 一致。下单扣库存为 `**trade-service` `@GlobalTransactional` + `item-service**`；当前共享配置为 `**data-source-proxy-mode: XA**`（若改为 AT，请保留 `hm-trade` / `hm-item` 的 `**undo_log**` 表，见 `docker/mysql/init/05-seata-undo-log.sql`）。更新 `shared-seata.yaml` 后需重新 `**nacos-seed**` 或在控制台手动发布。

## 2. 秒杀抢券（演示要点）

### 2.1 设计亮点

- **防超卖**：Redis Lua 原子脚本在同一事务内完成“判库存 + 扣库存 + 用户去重”
- **削峰落库**：抢券成功后只发 MQ，消费者异步写 `user_coupon`
- **两层限流**：\n  - 网关按券聚合（`RequestRateLimiter` + Redis）\n  - 服务按“用户+券”令牌桶（Redis Lua）

### 2.2 演示步骤（推荐用 Knife4j）

1. 打开促销文档：`http://localhost:8087/doc.html`\n2. 创建优惠券（管理端）\n3. 发布优惠券（会把库存同步到 Redis）\n4. 用用户身份调用 `POST /coupons/{id}/receive` 抢券\n5. 观察：2xx / 429（限流）/ 库存耗尽 的行为差异

## 3. k6 压测

脚本在 `scripts/k6/`。\n\n- 走网关：验证网关限流拦截（429）与保护下游\n- 直连促销：验证服务侧令牌桶与秒杀 Lua 行为\n\n详见 `scripts/k6/README.md`。

## 3.1 AI 导购助手（用户态 RAG）

- **回答逻辑与数据范围说明**：`ai-assistant-service/AI_ASSISTANT_LOGIC.md`（分流、RAG、工具、是否全量进上下文）
- **文档**：`http://localhost:8090/doc.html`
- **接口**：
  - `POST /ai/chat/sync`：同步返回
  - `POST /ai/chat`：SSE 流式返回（事件：`message` / `sources` / `done`）

### 演示步骤

1. 登录获取 JWT：`POST http://localhost:8080/users/login`
2. 调用 AI（走网关）：`POST http://localhost:8080/ai/chat/sync`\n   - Header：`Authorization: Bearer <JWT>`\n   - Body：`{\"message\":\"我有哪些优惠券？\"}` 或 `{\"message\":\"订单 123456789 状态？\"}` 或 `{\"message\":\"推荐 2000 元左右手机\"}`

### 配置百炼模型（推荐用根目录 `.env`）

根目录 `.env` 已预置以下变量（把 `HM_AI_LLM_API_KEY` 换成你自己的 Key）：\n- `HM_AI_LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode`\n- `HM_AI_LLM_MODEL=qwen-turbo-2025-07-15`\n\n**可选：百炼控制台创建智能体并绑定知识库后**，设置 `HM_AI_BAILIAN_APP_ID=<应用ID>`，通用导购将优先走百炼内置 RAG（失败时回退本地商品/券 RAG）。详见 `ai-assistant-service/AI_ASSISTANT_LOGIC.md`。\n\n启动（或重建）AI 服务：\n- `docker compose up -d --build ai-assistant-service`

## 4. 可观测性（Loki + Prometheus + Tempo + Grafana）

业务与观测栈均启动后，可集中看 **日志 / 指标 / 链路**（Sleuth **Zipkin** 上报 → Tempo，`traceId` 进 JSON 日志便于在 Grafana 中关联）。

### 4.1 启动顺序

1. 先按上文启动主栈：`docker compose up -d --build`。执行 `docker network ls` 查看业务容器所在网络：仓库内 [`docker-compose.observability.yml`](docker-compose.observability.yml) 默认使用外部网络名 **`hmall`**（与本仓库主栈自定义网络名一致时可直接 `compose up`）；若你环境是 **`hmall_hmall-net`** 等其它名称，请修改该文件末尾 `networks.hmall-net.name`。
2. 将 `[docker/nacos/init/shared-observability.yaml](docker/nacos/init/shared-observability.yaml)` 写入 Nacos（与主栈相同：执行一次 `nacos-seed` 或控制台手动发布）。
3. 启动观测栈：

```bash
docker compose -f docker-compose.yml -f docker-compose.observability.yml up -d
```

### 4.2 访问


| 组件         | 地址                                             | 说明                                               |
| ---------- | ---------------------------------------------- | ------------------------------------------------ |
| Grafana    | [http://localhost:3000](http://localhost:3000) | 默认账号 **admin / admin**                           |
| Prometheus | [http://localhost:9090](http://localhost:9090) | 指标抓取各服务 `http://<服务名>:端口/actuator/prometheus`    |
| Loki       | [http://localhost:3100](http://localhost:3100) | 由 Promtail 写入，一般在 Grafana Explore 使用             |
| Tempo      | [http://localhost:3200](http://localhost:3200) | 查询 API；**Zipkin 接收** 映射宿主机 **9411**（Sleuth 默认上报） |


### 4.3 本地 IDE 跑微服务（不跑在 Docker 里）

- 无 Tempo 时 Zipkin 客户端可能报连接失败：可临时关采样 `spring.sleuth.sampler.probability=0`，或本机也启动观测栈并将 `spring.zipkin.base-url` 指到 `http://127.0.0.1:9411`（需在 Nacos 覆盖 `shared-observability` 或使用 `TEMPO_HOST=127.0.0.1`，因 URL 模板为 `http://${TEMPO_HOST:tempo}:9411`）。

### 4.4 Windows / Docker Desktop 与 Promtail

- Promtail 挂载 `**/var/run/docker.sock`** 在 Docker Desktop 下通常可用；若日志进不了 Loki，可检查 `hmall-promtail` 容器日志，或暂时改用挂载业务目录日志的 static 配置（进阶）。

**勿将 `actuator/prometheus` 暴露在公网；仅内网或本机调试使用。**

## 5. 常见问题

- `**doc.html` 打不开 / 没接口**：检查 Nacos 中 `shared-swagger.yaml` 的占位符 `hm.swagger.*` 是否被各服务正确覆盖；或直接访问 `http://localhost:<port>/v2/api-docs` 看 `paths`。\n- **端口冲突**：修改 `docker-compose.yml` 的端口映射。\n- **数据库表缺失**：当前 Compose 仅内置 `hm-promotion` 的核心表初始化，其它服务表结构请按你环境补充 SQL（可逐步完善到 `docker/mysql/init/`）。\n
- **RabbitMQ `PRECONDITION_FAILED` / `inequivalent arg 'x-dead-letter-exchange'`**：说明 Broker 里已有**同名队列**但**没有**死信相关参数，与当前代码声明不一致。本项目已改用 **v2** 队列与路由（`coupon.receive.v2`、`promotion.coupon.receive.queue.v2`、`pay.success.v2`、`trade.pay.success.queue.v2`），一般**重启即可**。若仍在使用旧队列，可在管理台 **删除** 旧队列：`promotion.coupon.receive.queue`、`trade.pay.success.queue`（无消费者后删除），避免与 v2 并存时误绑同一路由造成重复消费。\n

