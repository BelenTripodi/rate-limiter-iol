package com.example.ratelimiter.infrastructure

object RedisScripts {
  val TOKEN_BUCKET_LUA: String = """
    local key = KEYS[1]
    local capacity = tonumber(ARGV[1])
    local refill_per_ms = tonumber(ARGV[2])
    local now = tonumber(ARGV[3])
    local cost = tonumber(ARGV[4])
    local ttl_ms = tonumber(ARGV[5])

    local data = redis.call('HMGET', key, 'tokens', 'ts')
    local tokens = tonumber(data[1])
    local ts = tonumber(data[2])

    if tokens == nil then
      tokens = capacity
      ts = now
    end

    local delta = now - ts
    if delta < 0 then delta = 0 end

    if refill_per_ms > 0 then
      tokens = tokens + (delta * refill_per_ms)
      if tokens > capacity then tokens = capacity end
    end

    local allowed = 0
    if tokens >= cost then
      allowed = 1
      tokens = tokens - cost
    end

    redis.call('HMSET', key, 'tokens', tokens, 'ts', now)
    redis.call('PEXPIRE', key, ttl_ms)

    local retry_after_ms = -1
    if allowed == 0 and refill_per_ms > 0 then
      retry_after_ms = math.ceil((cost - tokens) / refill_per_ms)
      if retry_after_ms < 0 then retry_after_ms = 0 end
    end

    return {allowed, tokens, retry_after_ms}
  """.trimIndent()
}
