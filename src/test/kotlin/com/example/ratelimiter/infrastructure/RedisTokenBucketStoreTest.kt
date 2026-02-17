package com.example.ratelimiter.infrastructure

import com.example.ratelimiter.domain.Clock
import com.example.ratelimiter.domain.RateLimitDecisionAllowed
import com.example.ratelimiter.domain.RateLimitDecisionDenied
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import java.time.Duration

class RedisTokenBucketStoreTest : FunSpec({

  test("allows when script returns allowed") {
    val redis = mockk<StringRedisTemplate>()
    val clock = mockk<Clock>()
    every { clock.nowMillis() } returns 1_000L

    every {
      redis.execute(
        any<DefaultRedisScript<List<Any>>>(),
        any<List<String>>(),
        any(), any(), any(), any(), any()
      )
    } returns listOf(1, 2.0, -1)

    val store = RedisTokenBucketStore(redis, clock)
    val result = store.tryConsume("k", 10, 1.0, 1)

    result shouldBe RateLimitDecisionAllowed(remainingTokens = 2)
  }

  test("denies with retryAfterMs when script returns denied") {
    val redis = mockk<StringRedisTemplate>()
    val clock = mockk<Clock>()
    every { clock.nowMillis() } returns 1_000L

    every {
      redis.execute(
        any<DefaultRedisScript<List<Any>>>(),
        any<List<String>>(),
        any(), any(), any(), any(), any()
      )
    } returns listOf(0, 0.5, 1200)

    val store = RedisTokenBucketStore(redis, clock)
    val result = store.tryConsume("k", 10, 1.0, 1)

    result shouldBe RateLimitDecisionDenied(retryAfterMs = 1200)
  }

  test("denied result with retryAfterMs -1 maps to Long.MAX_VALUE") {
    val redis = mockk<StringRedisTemplate>()
    val clock = mockk<Clock>()
    every { clock.nowMillis() } returns 1_000L

    every {
      redis.execute(
        any<DefaultRedisScript<List<Any>>>(),
        any<List<String>>(),
        any(), any(), any(), any(), any()
      )
    } returns listOf(0, 0.0, -1)

    val store = RedisTokenBucketStore(redis, clock)
    val result = store.tryConsume("k", 10, 1.0, 1)

    result shouldBe RateLimitDecisionDenied(retryAfterMs = Long.MAX_VALUE)
  }

  test("passes no-refill ttl when refill is 0") {
    val redis = mockk<StringRedisTemplate>()
    val clock = mockk<Clock>()
    every { clock.nowMillis() } returns 1_000L

    val ttlSlot = slot<String>()
    every {
      redis.execute(
        any<DefaultRedisScript<List<Any>>>(),
        any<List<String>>(),
        any(), any(), any(), any(), capture(ttlSlot)
      )
    } returns listOf(1, 1.0, -1)

    val store = RedisTokenBucketStore(redis, clock)
    store.tryConsume("k", 10, 0.0, 1)

    ttlSlot.captured shouldBe Duration.ofHours(1).toMillis().toString()
  }

  test("uses min ttl when refill is very fast") {
    val redis = mockk<StringRedisTemplate>()
    val clock = mockk<Clock>()
    every { clock.nowMillis() } returns 1_000L

    val ttlSlot = slot<String>()
    every {
      redis.execute(
        any<DefaultRedisScript<List<Any>>>(),
        any<List<String>>(),
        any(), any(), any(), any(), capture(ttlSlot)
      )
    } returns listOf(1, 1.0, -1)

    val store = RedisTokenBucketStore(redis, clock)
    store.tryConsume("k", 1, 1_000.0, 1)

    ttlSlot.captured shouldBe Duration.ofMinutes(1).toMillis().toString()
  }

  test("uses computed ttl when larger than min") {
    val redis = mockk<StringRedisTemplate>()
    val clock = mockk<Clock>()
    every { clock.nowMillis() } returns 1_000L

    val ttlSlot = slot<String>()
    every {
      redis.execute(
        any<DefaultRedisScript<List<Any>>>(),
        any<List<String>>(),
        any(), any(), any(), any(), capture(ttlSlot)
      )
    } returns listOf(1, 1.0, -1)

    val store = RedisTokenBucketStore(redis, clock)
    store.tryConsume("k", 120, 1.0, 1)

    ttlSlot.captured shouldBe "240000"
  }

  test("throws when script returns unexpected size") {
    val redis = mockk<StringRedisTemplate>()
    val clock = mockk<Clock>()
    every { clock.nowMillis() } returns 1_000L

    every {
      redis.execute(
        any<DefaultRedisScript<List<Any>>>(),
        any<List<String>>(),
        any(), any(), any(), any(), any()
      )
    } returns listOf(1, 2.0)

    val store = RedisTokenBucketStore(redis, clock)

    try {
      store.tryConsume("k", 10, 1.0, 1)
      throw AssertionError("Expected IllegalStateException")
    } catch (_: IllegalStateException) {
    }
  }
})
