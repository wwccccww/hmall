# 当前 AI 助手回答逻辑（与「能否读全量」说明）

本文与 `AiChatServiceImpl`、`SimpleRagService`、`UserTools` 实现保持一致，说明分流、RAG、工具与数据范围。

## 整体流程（两条主路径）

```mermaid
flowchart TD
  UserMsg[用户消息] --> ToolMatch{tryToolCall 关键词匹配}
  ToolMatch -->|命中| ToolPath[工具路径：查下游 + 可选 LLM 合成]
  ToolMatch -->|未命中| RAGPath[RAG：SimpleRagService]
  RAGPath --> LLM[LlmClient.chat 百炼兼容接口]
  LLM --> Answer[回答 + sources]
```

### 1) 工具路径（`tryToolCall`）

根据**固定中文关键词**触发，例如：

- 「我的优惠券 / 可用券 / 优惠券有哪些」→ `UserTools.queryMyCoupons`（`GET promotion-service /coupons/my`）
- 「我的地址 / 收货地址」→ `queryMyAddresses`（`GET user-service /addresses`，脱敏）
- 「我的信息 / 我是谁 / 我的账号」→ `queryMe`（`GET /users/me`，少量字段）
- 「订单 + 状态/查询/进度」且消息中正则匹配到**订单号** → `queryOrderById`

命中工具路径时：返回结构化 `sources`；若已配置 `HM_AI_LLM_API_KEY`，会**先工具后 LLM**，用查询结果合成自然语言回答；未配置或调用失败则使用预设短文案。SSE 分支发 `result` 事件。

### 2) 通用导购路径（RAG + LLM，`SimpleRagService`）

- **商品**：对 `item-service` 调用 `GET /items/page`，按配置 **`hm.ai.rag.item-max-pages`**（默认 2）连续拉取多页（每页 `pageSize=50`），合并后在本地用截断后的用户问题做关键词包含过滤；若无命中则用**前 8 条**兜底。
- **公开进行中优惠券**：对 `promotion-service` 调用 **`GET /coupons`**（免登录），取前 **`hm.ai.rag.public-coupon-max`**（默认 15）条，将规则摘要拼进 prompt，并写入 `sources`（`type=public_coupon`）。拉取失败则仅使用商品上下文。
- 把候选商品与券摘要拼进 prompt，规则写明须**仅基于所给信息**回答，再调用 `LlmClient.chat`。

**注意**：用户私有「我的优惠券」「地址」仍仅在工具关键词命中时查询，不会自动进入 RAG 路径。

---

## 「能否读完所有商品、优惠券、地址」？

| 数据 | 是否「全量」进入模型上下文 | 说明 |
|------|---------------------------|------|
| 商品 | **否** | 多页分页有上限；再筛最多 8 条进 prompt |
| 公开进行中券 | **否** | 仅 `GET /coupons` 摘要前 N 条 |
| 我的优惠券 | **仅关键词触发** | `/coupons/my`；工具路径可走 LLM 合成 |
| 地址 | **仅关键词触发** | 脱敏后字段有限 |

技术上可继续扩展分页或检索，但不建议把全库一次性塞进单次 prompt（token、成本、延迟、隐私）。

---

## 配置项（节选）

| 变量 / 配置 | 含义 |
|-------------|------|
| `HM_AI_ITEM_BASE_URL` / `hm.ai.rag.item-base-url` | 商品服务基址 |
| `HM_AI_PROMOTION_BASE_URL` / `hm.ai.rag.promotion-base-url` | 促销服务基址（公开券列表） |
| `hm.ai.rag.item-max-pages` | RAG 拉商品页数上限 |
| `hm.ai.rag.public-coupon-max` | 公开券写入 prompt 的最大条数 |
| `HM_AI_LLM_*` | 大模型 OpenAI 兼容接口 |
