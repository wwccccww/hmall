-- 令牌桶限流（单 key 原子）：按 userId + couponId 维度
-- KEYS[1]: Hash key，如 promotion:ratelimit:receive:{couponId}:{userId}
-- ARGV[1]: replenishRate，每秒补充令牌数（浮点）
-- ARGV[2]: burstCapacity，桶容量（整数）
-- 返回: 1=放行并扣 1 令牌，0=拒绝

local key = KEYS[1]
local rate = tonumber(ARGV[1])
local cap = tonumber(ARGV[2])
if rate == nil or cap == nil or rate <= 0 or cap <= 0 then
    return 1
end

local t = redis.call('TIME')
local nowMs = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)

local tokensStr = redis.call('HGET', key, 't')
local lastStr = redis.call('HGET', key, 'l')

local tokens
local lastMs
if tokensStr == false then
    tokens = cap
    lastMs = nowMs
else
    tokens = tonumber(tokensStr)
    if lastStr == false then
        lastMs = nowMs
    else
        lastMs = tonumber(lastStr)
    end
end

local elapsedSec = (nowMs - lastMs) / 1000.0
if elapsedSec < 0 then
    elapsedSec = 0
end

local newTokens = math.min(cap, tokens + elapsedSec * rate)
if newTokens >= 1 then
    newTokens = newTokens - 1
    redis.call('HSET', key, 't', tostring(newTokens))
    redis.call('HSET', key, 'l', tostring(nowMs))
    redis.call('EXPIRE', key, 86400)
    return 1
else
    redis.call('HSET', key, 't', tostring(newTokens))
    redis.call('HSET', key, 'l', tostring(nowMs))
    redis.call('EXPIRE', key, 86400)
    return 0
end
