package com.example.ratelimiter.application

import com.example.ratelimiter.config.RateLimiterFailStrategy
import com.example.ratelimiter.domain.RateLimitDecision
import com.example.ratelimiter.domain.RateLimitDecisionAllowed
import com.example.ratelimiter.domain.RateLimitDecisionDenied
import com.example.ratelimiter.domain.TokenBucketStore
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory

class RateLimiterEngine(
  private val store: TokenBucketStore,
  private val resolver: PolicyResolver,
  private val failStrategy: RateLimiterFailStrategy
) {
  private val log = LoggerFactory.getLogger(javaClass)

  fun evaluate(request: HttpServletRequest, identities: RequestIdentityContext): EvaluationResult {
    val path = request.requestURI ?: "/"
    val policies = resolver.resolve(path)
    if (policies.isEmpty()) {
      return EvaluationAllowed(
        remainingTokens = Long.MAX_VALUE,
        limit = Long.MAX_VALUE,
        refillTokensPerSecond = null
      )
    }

    var minRemaining = Long.MAX_VALUE
    var maxRetryAfterMs = 0L
    var minCapacity = Long.MAX_VALUE
    var minRefillPerSecond = Double.MAX_VALUE

    try {
      policies.forEach { policy ->
        minCapacity = minOf(minCapacity, policy.capacity)
        minRefillPerSecond = minOf(minRefillPerSecond, policy.refillTokensPerSecond)
        val identity = identities.forStrategy(policy.keyStrategy)
        val bucketKey = bucketKey(policy.name, identity)
        when (val decision = store.tryConsume(bucketKey, policy.capacity, policy.refillTokensPerSecond, policy.cost)) {
          is RateLimitDecisionAllowed -> minRemaining = minOf(minRemaining, decision.remainingTokens)
          is RateLimitDecisionDenied -> maxRetryAfterMs = maxOf(maxRetryAfterMs, decision.retryAfterMs)
        }
      }
    } catch (e: Exception) {
      log.warn("Rate limiter store error (strategy=$failStrategy). path=$path", e)
      return when (failStrategy) {
        RateLimiterFailStrategy.FAIL_OPEN ->
          EvaluationAllowed(
            remainingTokens = Long.MAX_VALUE,
            limit = minCapacity,
            refillTokensPerSecond = minRefillPerSecond,
            degraded = true
          )
        RateLimiterFailStrategy.FAIL_CLOSED ->
          EvaluationDenied(
            retryAfterMs = 1_000,
            limit = minCapacity,
            refillTokensPerSecond = minRefillPerSecond,
            degraded = true
          )
      }
    }

    return if (maxRetryAfterMs > 0) {
      EvaluationDenied(
        retryAfterMs = maxRetryAfterMs,
        limit = minCapacity,
        refillTokensPerSecond = minRefillPerSecond
      )
    } else {
      EvaluationAllowed(
        remainingTokens = minRemaining,
        limit = minCapacity,
        refillTokensPerSecond = minRefillPerSecond
      )
    }
  }

  private fun bucketKey(ruleName: String, identity: RequestIdentity): String =
    "$ruleName:${identity.type}:${identity.value}"

}
