package com.example.ratelimiter.infrastructure

import com.example.ratelimiter.domain.Clock
import com.example.ratelimiter.domain.RateLimitDecision
import com.example.ratelimiter.domain.RateLimitDecisionAllowed
import com.example.ratelimiter.domain.RateLimitDecisionDenied
import com.example.ratelimiter.domain.TokenBucketStore
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Thread-safe, in-process token bucket store.
 *
 * Each key has its own lock to reduce contention under concurrency.
 */
class InMemoryTokenBucketStore(private val clock: Clock) : TokenBucketStore {

  private val buckets = ConcurrentHashMap<String, BucketState>()

  override fun tryConsume(
    key: String,
    capacity: Long,
    refillTokensPerSecond: Double,
    cost: Long
  ): RateLimitDecision {
    require(capacity > 0) { "capacity must be > 0" }
    require(cost > 0) { "cost must be > 0" }

    val now = clock.nowMillis()
    val state = buckets.computeIfAbsent(key) { BucketState(tokens = capacity.toDouble(), lastRefillMs = now) }

    state.lock.lock()
    try {
      val refillPerMs = refillTokensPerSecond / 1000.0
      val elapsed = max(0L, now - state.lastRefillMs)
      val refill = elapsed * refillPerMs

      state.tokens = min(capacity.toDouble(), state.tokens + refill)
      state.lastRefillMs = now

      val allowed = state.tokens >= cost
      return if (allowed) {
        state.tokens -= cost
        RateLimitDecisionAllowed(remainingTokens = state.tokens.toLong())
      } else {
        val retryAfterMs =
          if (refillPerMs > 0) ceil((cost - state.tokens) / refillPerMs).toLong()
          else Long.MAX_VALUE
        RateLimitDecisionDenied(retryAfterMs = retryAfterMs)
      }
    } finally {
      state.lock.unlock()
    }
  }
}
