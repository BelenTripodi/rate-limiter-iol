package com.example.ratelimiter.infrastructure

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RedisScriptResultTest : FunSpec({

  test("data class stores values") {
    val result = RedisScriptResult(allowed = true, tokens = 1.5, retryAfterMs = 10)
    result.allowed shouldBe true
    result.tokens shouldBe 1.5
    result.retryAfterMs shouldBe 10
  }
})
