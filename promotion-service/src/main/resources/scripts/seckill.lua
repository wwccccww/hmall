-- 限量优惠券秒杀原子脚本（Lua 保证判断 + 扣减的原子性）
-- KEYS[1]: 库存 key       promotion:coupon:stock:{couponId}
-- KEYS[2]: 已领用户 Set   promotion:coupon:user:{couponId}
-- ARGV[1]: 当前用户 ID（String）
--
-- 返回值:
--   0 → 成功
--   1 → 库存不足
--   2 → 重复领取（每人限一张）

-- 1. 查询当前库存
local stock = tonumber(redis.call('get', KEYS[1]))
if stock == nil or stock <= 0 then
    return 1
end

-- 2. 判断用户是否已领取
if redis.call('sismember', KEYS[2], ARGV[1]) == 1 then
    return 2
end

-- 3. 原子扣减库存并记录用户
redis.call('incrby', KEYS[1], -1)
redis.call('sadd', KEYS[2], ARGV[1])
return 0
