package com.example.ratelimiter.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PolicyDataTest : FunSpec({

  test("data class supports copy and component access") {
    val policy = Policy(
      name = "p",
      pathPattern = "/**",
      keyStrategy = PolicyKeyStrategy.API_KEY,
      capacity = 10,
      refillTokensPerSecond = 1.0,
      cost = 1
    )

    val copy = policy.copy()
    copy shouldBe policy
    policy.component1() shouldBe "p"
    policy.component3() shouldBe PolicyKeyStrategy.API_KEY
    policy.toString().contains("Policy") shouldBe true
    policy.hashCode() shouldBe copy.hashCode()
  }
})
