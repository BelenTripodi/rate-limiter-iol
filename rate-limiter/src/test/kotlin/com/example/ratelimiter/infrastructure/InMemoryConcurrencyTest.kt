package com.example.ratelimiter.infrastructure

import com.example.ratelimiter.domain.RateLimitDecisionAllowed
import com.example.ratelimiter.domain.RateLimitDecisionDenied
import com.example.ratelimiter.testutil.TestClock
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class InMemoryConcurrencyTest : FunSpec({

  test("concurrent calls do not allow more than capacity when refill rate is 0") {
    val clock = TestClock(0)
    val store = InMemoryTokenBucketStore(clock)

    val capacity = 10L
    val rate = 0.0
    val key = "concurrent"

    val exec = Executors.newFixedThreadPool(16)
    try {
      val tasks = (1..100).map { Callable { store.tryConsume(key, capacity, rate, 1) } }
      val results = exec.invokeAll(tasks).map { it.get() }

      val allowed = results.count { it is RateLimitDecisionAllowed }
      val denied = results.count { it is RateLimitDecisionDenied }

      allowed shouldBe capacity.toInt()
      denied shouldBe 100 - capacity.toInt()
    } finally {
      exec.shutdownNow()
    }
  }
})
