package com.example.ratelimiter.infrastructure

import com.example.ratelimiter.domain.RateLimitDecisionAllowed
import com.example.ratelimiter.domain.RateLimitDecisionDenied
import com.example.ratelimiter.testutil.TestClock
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class InMemoryTokenBucketStoreTest : FunSpec({

  test("allows up to capacity immediately") {
    val clock = TestClock(0)
    val store = InMemoryTokenBucketStore(clock)

    val key = "k"
    val cap = 3L
    val rate = 0.0

    store.tryConsume(key, cap, rate, 1) shouldBe RateLimitDecisionAllowed(2)
    store.tryConsume(key, cap, rate, 1) shouldBe RateLimitDecisionAllowed(1)
    store.tryConsume(key, cap, rate, 1) shouldBe RateLimitDecisionAllowed(0)
    (store.tryConsume(key, cap, rate, 1) is RateLimitDecisionDenied) shouldBe true
  }

  test("refills tokens over time") {
    val clock = TestClock(0)
    val store = InMemoryTokenBucketStore(clock)

    val key = "k2"
    val cap = 10L
    val rate = 1.0 // 1 token/sec

    repeat(10) { store.tryConsume(key, cap, rate, 1) }
    (store.tryConsume(key, cap, rate, 1) is RateLimitDecisionDenied) shouldBe true

    clock.advanceMillis(1_000) // +1 token
    store.tryConsume(key, cap, rate, 1) shouldBe RateLimitDecisionAllowed(0)
  }

  test("never exceeds capacity on refill") {
    val clock = TestClock(0)
    val store = InMemoryTokenBucketStore(clock)

    val key = "k3"
    val cap = 5L
    val rate = 10.0 // 10 tokens/sec

    store.tryConsume(key, cap, rate, 1) shouldBe RateLimitDecisionAllowed(4)

    clock.advanceMillis(10_000) // enough to overfill
    val decision = store.tryConsume(key, cap, rate, 1) as RateLimitDecisionAllowed
    decision.remainingTokens shouldBe 4
  }

  test("rejects invalid capacity") {
    val clock = TestClock(0)
    val store = InMemoryTokenBucketStore(clock)

    try {
      store.tryConsume("k", 0, 1.0, 1)
      throw AssertionError("Expected IllegalArgumentException")
    } catch (_: IllegalArgumentException) {
    }
  }

  test("rejects invalid cost") {
    val clock = TestClock(0)
    val store = InMemoryTokenBucketStore(clock)

    try {
      store.tryConsume("k", 1, 1.0, 0)
      throw AssertionError("Expected IllegalArgumentException")
    } catch (_: IllegalArgumentException) {
    }
  }
})
