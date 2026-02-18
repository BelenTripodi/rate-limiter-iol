package com.example.ratelimiter.infrastructure

import com.example.ratelimiter.domain.Clock
import com.example.ratelimiter.domain.RateLimitDecision
import com.example.ratelimiter.domain.RateLimitDecisionAllowed
import com.example.ratelimiter.domain.RateLimitDecisionDenied
import com.example.ratelimiter.domain.TokenBucketStore
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.math.ceil

private val NO_REFILL_TTL_MS: Long = Duration.ofHours(1).toMillis()

class RedisTokenBucketStore(
  private val redis: StringRedisTemplate,
  private val clock: Clock
) : TokenBucketStore {

  private val log = LoggerFactory.getLogger(javaClass)

  private val script = DefaultRedisScript<List<Any>>().apply {
    setScriptText(RedisScripts.TOKEN_BUCKET_LUA)
    @Suppress("UNCHECKED_CAST")
    resultType = List::class.java as Class<List<Any>>
  }

  override fun tryConsume(
    key: String,
    capacity: Long,
    refillTokensPerSecond: Double,
    cost: Long
  ): RateLimitDecision {
    val now = clock.nowMillis()
    val refillPerMs = refillTokensPerSecond / 1000.0
    val ttlMs = computeTtlMs(capacity, refillTokensPerSecond)

    val result = requireNotNull(redis.execute(
      script,
      listOf(key),
      capacity.toString(),
      refillPerMs.toString(),
      now.toString(),
      cost.toString(),
      ttlMs.toString()
    )) { "Redis script returned null" }.toScriptResult()

    return if (result.allowed) {
      RateLimitDecisionAllowed(remainingTokens = result.tokens.toLong())
    } else {
      val retryAfterMs =
        if (result.retryAfterMs >= 0) result.retryAfterMs
        else Long.MAX_VALUE
      log.info("Rate limit denied (redis). policy={} retryAfterMs={}", policyName(key), retryAfterMs)
      RateLimitDecisionDenied(retryAfterMs = retryAfterMs)
    }
  }

  private fun List<Any>.toScriptResult(): RedisScriptResult {
    check(size == 3) { "Unexpected Redis script result size: $size" }
    val (allowedRaw, tokensRaw, retryRaw) = this
    val allowed = (allowedRaw as Number).toLong() == 1L
    val tokens = (tokensRaw as Number).toDouble()
    val retryAfterMs = (retryRaw as Number).toLong()
    return RedisScriptResult(allowed = allowed, tokens = tokens, retryAfterMs = retryAfterMs)
  }

  private fun computeTtlMs(capacity: Long, refillTokensPerSecond: Double): Long {
    if (refillTokensPerSecond <= 0.0) return NO_REFILL_TTL_MS
    val secondsToFull = capacity / refillTokensPerSecond
    val minTtlMs = Duration.ofMinutes(1).toMillis()
    val ttlMs = ceil(secondsToFull * 2 * 1000.0).toLong()
    return maxOf(minTtlMs, ttlMs)
  }

  private fun policyName(key: String): String =
    key.substringBefore(":")
}
