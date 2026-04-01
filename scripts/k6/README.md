# k6 秒杀抢券压测脚本

本目录用于对「领取优惠券（秒杀）」接口做压测，与项目中的网关限流、服务内 Redis 令牌桶脚本配套使用。

## 前置条件

- 已安装 [k6](https://k6.io/docs/get-started/installation/)
- 已有一张**已发布**、有库存的券，记下 `COUPON_ID`
- Redis、网关、promotion-service 已按环境启动

## 脚本说明

| 文件 | 作用 |
|------|------|
| `seckill-receive-gateway.js` | 请求 **hm-gateway**（默认 `http://localhost:8080`），带 JWT，会经过网关 `RequestRateLimiter` |
| `seckill-receive-direct.js` | **直连 promotion-service**（默认 `http://localhost:8087`），只带 `user-info` 头，**不经过网关限流** |

## 走网关（推荐）

### 单用户（所有 VU 共用一个 JWT）

适合测「同一账号高并发」或服务内按 userId 的限流桶。

```powershell
cd d:\1study\study\java\program\hmall
$env:COUPON_ID="你的券ID"
$env:TOKEN="你的JWT"
# 可选：$env:BASE_URL="http://localhost:8080"
k6 run scripts/k6/seckill-receive-gateway.js
```

### 多用户（每个 VU 使用不同账号的 JWT）

1. 准备多个测试账号并分别登录，拿到多份 JWT。
2. 复制 [`tokens.sample.json`](tokens.sample.json) 为 `tokens.json`（建议勿把真实 JWT 提交到 Git），填入 `tokens` 数组。
3. 在**仓库根目录**执行（`TOKENS_FILE` 相对当前工作目录）：

```powershell
cd d:\1study\study\java\program\hmall
$env:COUPON_ID="你的券ID"
$env:TOKENS_FILE="scripts/k6/tokens.json"
k6 run scripts/k6/seckill-receive-gateway.js
```

脚本按 **VU 编号** 轮询选择 token：`VU 1 → tokens[0]`，`VU 2 → tokens[1]`…若 VU 数量大于 token 个数，会从头复用（多个 VU 共用同一账号）。token 个数建议 **不少于目标峰值 VU 数**，才能接近「人手一号」。

也支持 JSON 直接为字符串数组：`["jwt1","jwt2"]`。

若 JWT 本身已含 `Bearer ` 前缀，可直接写入 JSON，脚本会原样使用。

## 直连 promotion-service（对比用）

```powershell
cd d:\1study\study\java\program\hmall
$env:COUPON_ID="你的券ID"
# 可选：$env:BASE_URL="http://localhost:8087"
# 可选：$env:USER_BASE="10000000"
k6 run scripts/k6/seckill-receive-direct.js
```

每个虚拟用户使用不同 `user-id`（`USER_BASE + VU * 1e6 + 迭代号`），便于模拟多用户抢同一券。

## 结果解读（简要）

- **2xx 数量**：近似不超过券库存（还要扣掉未开始/已结束等非 2xx 业务情况）。
- **429**：网关或服务侧令牌桶限流生效。
- Windows 本机高并发若出现 `connectex` / 端口相关错误，可适当增大脚本中的 `sleep`，或降低 `target` VUs，或将 k6 与业务拆到不同机器。

## 与配置项的对应关系

- 网关：`hm.ratelimit.receive.replenish` / `burst`（见 `hm-gateway` 的 `application.yaml`）。
- 服务：`hm.seckill.receive-rate-limit`（见 `promotion-service` 的 `application-local.yaml`）。

若 Nacos 动态路由覆盖了网关本地路由，需在 `gateway-routes.json` 中为 `POST /coupons/*/receive` 同步配置 `RequestRateLimiter`，否则网关限流可能不生效。
