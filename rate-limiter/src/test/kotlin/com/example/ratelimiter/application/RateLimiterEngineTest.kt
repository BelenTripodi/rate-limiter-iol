package com.example.ratelimiter.application

import com.example.ratelimiter.config.RateLimiterFailStrategy
import com.example.ratelimiter.config.RateLimiterKeyStrategy
import com.example.ratelimiter.config.RateLimiterMode
import com.example.ratelimiter.config.RateLimiterProperties
import com.example.ratelimiter.config.RuleProperties
import com.example.ratelimiter.domain.RateLimitDecision
import com.example.ratelimiter.domain.RateLimitDecisionAllowed
import com.example.ratelimiter.domain.RateLimitDecisionDenied
import com.example.ratelimiter.domain.TokenBucketStore
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest

class RateLimiterEngineTest : FunSpec({

  test("allows when no policies match") {
    val props = RateLimiterProperties(
      mode = RateLimiterMode.IN_MEMORY,
      failStrategy = RateLimiterFailStrategy.FAIL_CLOSED,
      rules = emptyList()
    )
    val resolver = PolicyResolver(props)
    val store = mockk<TokenBucketStore>()
    val engine = RateLimiterEngine(store, resolver, RateLimiterFailStrategy.FAIL_CLOSED)

    val request = mockk<HttpServletRequest>()
    every { request.requestURI } returns "/no-rules"

    val identities = RequestIdentityContext(
      apiKey = RequestIdentity(RequestIdentityType.API_KEY, "k"),
      ip = RequestIdentity(RequestIdentityType.IP, "1.1.1.1")
    )

    val result = engine.evaluate(request, identities) as EvaluationAllowed
    result.remainingTokens shouldBe Long.MAX_VALUE
    result.limit shouldBe Long.MAX_VALUE
    result.refillTokensPerSecond shouldBe null
  }

  test("uses default path when request URI is null") {
    val props = RateLimiterProperties(
      mode = RateLimiterMode.IN_MEMORY,
      failStrategy = RateLimiterFailStrategy.FAIL_CLOSED,
      rules = emptyList()
    )
    val resolver = PolicyResolver(props)
    val store = mockk<TokenBucketStore>()
    val engine = RateLimiterEngine(store, resolver, RateLimiterFailStrategy.FAIL_CLOSED)

    val request = mockk<HttpServletRequest>()
    every { request.requestURI } returns null

    val identities = RequestIdentityContext(
      apiKey = RequestIdentity(RequestIdentityType.API_KEY, "k"),
      ip = RequestIdentity(RequestIdentityType.IP, "1.1.1.1")
    )

    val result = engine.evaluate(request, identities) as EvaluationAllowed
    result.limit shouldBe Long.MAX_VALUE
  }

  test("denies when any policy is denied with max retry after") {
    val props = RateLimiterProperties(
      mode = RateLimiterMode.IN_MEMORY,
      failStrategy = RateLimiterFailStrategy.FAIL_CLOSED,
      rules = listOf(
        RuleProperties(
          name = "r1",
          pathPattern = "/**",
          keyStrategy = RateLimiterKeyStrategy.API_KEY,
          capacity = 10,
          refillTokensPerSecond = 2.0,
          cost = 1
        ),
        RuleProperties(
          name = "r2",
          pathPattern = "/**",
          keyStrategy = RateLimiterKeyStrategy.API_KEY,
          capacity = 5,
          refillTokensPerSecond = 1.0,
          cost = 1
        )
      )
    )

    val resolver = PolicyResolver(props)
    val store = object : TokenBucketStore {
      override fun tryConsume(
        key: String,
        capacity: Long,
        refillTokensPerSecond: Double,
        cost: Long
      ): RateLimitDecision =
        if (key.startsWith("r2")) RateLimitDecisionDenied(retryAfterMs = 1500)
        else RateLimitDecisionAllowed(remainingTokens = 7)
    }

    val engine = RateLimiterEngine(store, resolver, RateLimiterFailStrategy.FAIL_CLOSED)
    val request = mockk<HttpServletRequest>()
    every { request.requestURI } returns "/anything"

    val identities = RequestIdentityContext(
      apiKey = RequestIdentity(RequestIdentityType.API_KEY, "k"),
      ip = RequestIdentity(RequestIdentityType.IP, "1.1.1.1")
    )

    val result = engine.evaluate(request, identities) as EvaluationDenied
    result.retryAfterMs shouldBe 1500
    result.limit shouldBe 5
    result.refillTokensPerSecond shouldBe 1.0
  }

  test("allows with min remaining when all policies allow") {
    val props = RateLimiterProperties(
      mode = RateLimiterMode.IN_MEMORY,
      failStrategy = RateLimiterFailStrategy.FAIL_CLOSED,
      rules = listOf(
        RuleProperties(
          name = "r1",
          pathPattern = "/**",
          keyStrategy = RateLimiterKeyStrategy.API_KEY,
          capacity = 10,
          refillTokensPerSecond = 2.0,
          cost = 1
        ),
        RuleProperties(
          name = "r2",
          pathPattern = "/**",
          keyStrategy = RateLimiterKeyStrategy.API_KEY,
          capacity = 4,
          refillTokensPerSecond = 1.0,
          cost = 1
        )
      )
    )

    val resolver = PolicyResolver(props)
    val store = object : TokenBucketStore {
      override fun tryConsume(
        key: String,
        capacity: Long,
        refillTokensPerSecond: Double,
        cost: Long
      ): RateLimitDecision =
        if (key.startsWith("r1")) RateLimitDecisionAllowed(remainingTokens = 9)
        else RateLimitDecisionAllowed(remainingTokens = 3)
    }

    val engine = RateLimiterEngine(store, resolver, RateLimiterFailStrategy.FAIL_CLOSED)
    val request = mockk<HttpServletRequest>()
    every { request.requestURI } returns "/anything"

    val identities = RequestIdentityContext(
      apiKey = RequestIdentity(RequestIdentityType.API_KEY, "k"),
      ip = RequestIdentity(RequestIdentityType.IP, "1.1.1.1")
    )

    val result = engine.evaluate(request, identities) as EvaluationAllowed
    result.remainingTokens shouldBe 3
    result.limit shouldBe 4
    result.refillTokensPerSecond shouldBe 1.0
  }

  test("fail-open on store exception when configured") {
    val props = RateLimiterProperties(
      mode = RateLimiterMode.IN_MEMORY,
      failStrategy = RateLimiterFailStrategy.FAIL_OPEN,
      rules = listOf(
        RuleProperties(
          name = "r1",
          pathPattern = "/**",
          keyStrategy = RateLimiterKeyStrategy.API_KEY,
          capacity = 10,
          refillTokensPerSecond = 2.0,
          cost = 1
        )
      )
    )

    val resolver = PolicyResolver(props)
    val store = object : TokenBucketStore {
      override fun tryConsume(
        key: String,
        capacity: Long,
        refillTokensPerSecond: Double,
        cost: Long
      ): RateLimitDecision {
        throw IllegalStateException("boom")
      }
    }

    val engine = RateLimiterEngine(store, resolver, RateLimiterFailStrategy.FAIL_OPEN)
    val request = mockk<HttpServletRequest>()
    every { request.requestURI } returns "/anything"

    val identities = RequestIdentityContext(
      apiKey = RequestIdentity(RequestIdentityType.API_KEY, "k"),
      ip = RequestIdentity(RequestIdentityType.IP, "1.1.1.1")
    )

    val result = engine.evaluate(request, identities) as EvaluationAllowed
    result.degraded shouldBe true
    result.limit shouldBe 10
    result.refillTokensPerSecond shouldBe 2.0
  }

  test("fail-closed on store exception when configured") {
    val props = RateLimiterProperties(
      mode = RateLimiterMode.IN_MEMORY,
      failStrategy = RateLimiterFailStrategy.FAIL_CLOSED,
      rules = listOf(
        RuleProperties(
          name = "r1",
          pathPattern = "/**",
          keyStrategy = RateLimiterKeyStrategy.API_KEY,
          capacity = 10,
          refillTokensPerSecond = 2.0,
          cost = 1
        )
      )
    )

    val resolver = PolicyResolver(props)
    val store = object : TokenBucketStore {
      override fun tryConsume(
        key: String,
        capacity: Long,
        refillTokensPerSecond: Double,
        cost: Long
      ): RateLimitDecision {
        throw IllegalStateException("boom")
      }
    }

    val engine = RateLimiterEngine(store, resolver, RateLimiterFailStrategy.FAIL_CLOSED)
    val request = mockk<HttpServletRequest>()
    every { request.requestURI } returns "/anything"

    val identities = RequestIdentityContext(
      apiKey = RequestIdentity(RequestIdentityType.API_KEY, "k"),
      ip = RequestIdentity(RequestIdentityType.IP, "1.1.1.1")
    )

    val result = engine.evaluate(request, identities) as EvaluationDenied
    result.degraded shouldBe true
    result.limit shouldBe 10
    result.refillTokensPerSecond shouldBe 2.0
  }
})
