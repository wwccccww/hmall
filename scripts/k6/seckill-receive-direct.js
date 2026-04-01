/**
 * 秒杀抢券压测：直连 promotion-service（默认 8087），不走网关。
 *
 * 仅注入 user-info 模拟用户，适合对比「无网关 RequestRateLimiter」时的行为。
 * 生产/完整链路压测请用 seckill-receive-gateway.js。
 *
 * 环境变量：
 *   COUPON_ID  必填
 *   BASE_URL   可选，默认 http://localhost:8087
 *   USER_BASE  可选，用户 ID 基数，默认 10000000
 *
 * 示例：
 *   $env:COUPON_ID="xxx"; k6 run scripts/k6/seckill-receive-direct.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    ramp: {
      executor: 'ramping-vus',
      startVUs: 50,
      stages: [
        { duration: '10s', target: 300 },
        { duration: '20s', target: 800 },
        { duration: '10s', target: 0 },
      ],
    },
  },
  noConnectionReuse: false,
  noVUConnectionReuse: false,
};

const couponId = __ENV.COUPON_ID;
const baseUrl = (__ENV.BASE_URL || 'http://192.168.59.65:8087').replace(/\/$/, '');
const userBase = parseInt(__ENV.USER_BASE || '10000000', 10);

if (!couponId) {
  throw new Error('请设置环境变量 COUPON_ID');
}

export default function () {
  const userId = userBase + __VU * 1000000 + __ITER;
  const url = `${baseUrl}/coupons/${couponId}/receive`;

  const res = http.post(url, null, {
    headers: {
      'user-info': String(userId),
    },
  });

  check(res, {
    '2xx 成功': (r) => r.status >= 200 && r.status < 300,
    '429 限流': (r) => r.status === 429,
  });

  sleep(0.05);
}
