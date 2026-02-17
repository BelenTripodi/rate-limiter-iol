package com.example.ratelimiter.application

import com.example.ratelimiter.config.RateLimiterKeyStrategy
import com.example.ratelimiter.config.RateLimiterProperties
import com.example.ratelimiter.config.RateLimiterMode
import com.example.ratelimiter.config.RateLimiterFailStrategy
import com.example.ratelimiter.config.RuleProperties
import com.example.ratelimiter.domain.PolicyKeyStrategy
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PolicyResolverTest : FunSpec({

  test("resolves matching policies by path pattern") {
    val props = RateLimiterProperties(
      mode = RateLimiterMode.IN_MEMORY,
      failStrategy = RateLimiterFailStrategy.FAIL_CLOSED,
      rules = listOf(
        RuleProperties(
          name = "global",
          pathPattern = "/**",
          keyStrategy = RateLimiterKeyStrategy.API_KEY,
          capacity = 10,
          refillTokensPerSecond = 1.0,
          cost = 1
        ),
        RuleProperties(
          name = "search",
          pathPattern = "/v1/search/**",
          keyStrategy = RateLimiterKeyStrategy.IP,
          capacity = 5,
          refillTokensPerSecond = 0.5,
          cost = 2
        )
      )
    )

    val resolver = PolicyResolver(props)
    val policies = resolver.resolve("/v1/search/abc")

    policies.size shouldBe 2
    policies[0].name shouldBe "global"
    policies[0].keyStrategy shouldBe PolicyKeyStrategy.API_KEY
    policies[1].name shouldBe "search"
    policies[1].keyStrategy shouldBe PolicyKeyStrategy.IP
  }

  test("returns empty list when no patterns match") {
    val props = RateLimiterProperties(
      mode = RateLimiterMode.IN_MEMORY,
      failStrategy = RateLimiterFailStrategy.FAIL_CLOSED,
      rules = listOf(
        RuleProperties(
          name = "only-search",
          pathPattern = "/v1/search/**",
          keyStrategy = RateLimiterKeyStrategy.API_KEY,
          capacity = 10,
          refillTokensPerSecond = 1.0,
          cost = 1
        )
      )
    )

    val resolver = PolicyResolver(props)
    resolver.resolve("/health") shouldBe emptyList()
  }
})
