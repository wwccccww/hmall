/**
 * 秒杀抢券压测：经 hm-gateway（默认 8080）。
 *
 * 依赖：JWT。网关 AuthGlobalFilter 会校验 Authorization。
 *
 * 环境变量：
 *   COUPON_ID    必填，券 ID
 *   BASE_URL     可选，默认 http://localhost:8080
 *
 * 身份（二选一）：
 *   TOKEN        单用户：所有 VU 共用同一 JWT
 *   TOKENS_FILE  多用户：指向 JSON 文件路径（相对当前工作目录，建议在仓库根目录执行 k6）
 *
 * JSON 格式（任选其一）：
 *   ["jwt1", "jwt2"]
 *   { "tokens": ["jwt1", "jwt2"] }
 *
 * 多用户时：按 VU 号轮询取 token，即 VU1→tokens[0]、VU2→tokens[1]…（VU 数大于 token 个数时会重复）。
 *
 * 示例：
 *   单用户：$env:TOKEN="..."; k6 run scripts/k6/seckill-receive-gateway.js
 *   多用户：
 *  $env:TOKENS_FILE="scripts/k6/tokens.sample.json"; 
 *  $env:COUPON_ID="2038961321487638531";
 *  k6 run scripts/k6/seckill-receive-gateway.js
 */

/*
单用户压测示例：
 $env:TOKEN=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ1c2VyIjoyMCwiZXhwIjoxNzc1MDEyMTQyfQ.FQMqiAqspiF8KdOZ3EUpgEcIMRG8XP6MLhYNUDF36mL7tHCT4y0ynC0fcz45QGtd5fmkQUzaTu6eQVT8EZLNXcJ3o6PSFyl8zi11bFxrv12D7WIC3pHM_QncdATUexrqweWREMinrr47W8GG5Z9nMDtaztkF7JzSK0VZ9c9patxJKZkPAStgvxIxE3G4YOAY8Cqzi6IgtwBRe7uGaiYtr9mT3vjIqDwjWBopvB8PBxOrcZ7TSFA61_OsI1Cki8CejkbgKKMENKIvE_rdqRuPseWXQf9eDWLYsKoFwXvtIPCHCKuVIo83If3m2ise4MKLs6bCBe4OAFpewdyAH3my5g"; 
 $env:COUPON_ID="2038961321487638531";
 k6 run scripts/k6/seckill-receive-gateway.js

多用户压测示例：
 $env:TOKENS_FILE="scripts/k6/tokens.sample.json"; 
 $env:COUPON_ID="2038961321487638531";
 k6 run scripts/k6/seckill-receive-gateway.js

*/
import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

export const options = {
  scenarios: {
    ramp: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { duration: '10s', target: 50 },
        { duration: '20s', target: 200 },
        { duration: '10s', target: 0 },
      ],
    },
  },
  noConnectionReuse: false,
  noVUConnectionReuse: false,
};

const couponId = __ENV.COUPON_ID;
const baseUrl = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const tokensFile = __ENV.TOKENS_FILE;
const singleToken = __ENV.TOKEN;

if (!couponId) {
  throw new Error('请设置环境变量 COUPON_ID');
}

/** @type {string[]} */
let tokenList;

if (tokensFile) {
  tokenList = new SharedArray('gateway_jwts', function () {
    const raw = open(tokensFile);
    const data = JSON.parse(raw);
    if (Array.isArray(data)) {
      return data.map((x) => String(x).trim()).filter((s) => s.length > 0);
    }
    if (data && Array.isArray(data.tokens)) {
      return data.tokens.map((x) => String(x).trim()).filter((s) => s.length > 0);
    }
    throw new Error('TOKENS_FILE JSON 需为字符串数组或 { "tokens": [...] }');
  });
  if (!tokenList.length) {
    throw new Error('TOKENS_FILE 解析后没有有效 token');
  }
} else if (singleToken) {
  tokenList = [singleToken.trim()];
} else {
  throw new Error('请设置 TOKEN（单用户）或 TOKENS_FILE（多用户 JSON 路径）');
}

function bearer(token) {
  const t = token.trim();
  return t.startsWith('Bearer ') ? t : `Bearer ${t}`;
}

/**
 * 按 VU 选用户：同一 VU 多次迭代使用同一 JWT，更接近「多名真实用户各点多次」。
 * 若希望每次迭代换用户，可改为 (__VU + __ITER) % tokenList.length。
 */
function tokenForVu(vu) {
  const idx = (vu - 1) % tokenList.length;
  return tokenList[idx];
}

export default function () {
  const url = `${baseUrl}/coupons/${couponId}/receive`;
  const authHeader = bearer(tokenForVu(__VU));

  const res = http.post(url, null, {
    headers: {
      Authorization: authHeader,
    },
  });

  check(res, {
    '2xx 成功': (r) => r.status >= 200 && r.status < 300,
    '429 限流': (r) => r.status === 429,
  });

  sleep(0.05);
}
