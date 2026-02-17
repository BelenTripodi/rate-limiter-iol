package com.example.ratelimiter.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PolicyTest : FunSpec({

  test("policy data class stores values") {
    val policy = Policy(
      name = "p",
      pathPattern = "/**",
      keyStrategy = PolicyKeyStrategy.API_KEY,
      capacity = 10,
      refillTokensPerSecond = 1.0,
      cost = 1
    )

    policy.name shouldBe "p"
    policy.keyStrategy shouldBe PolicyKeyStrategy.API_KEY
  }
})
