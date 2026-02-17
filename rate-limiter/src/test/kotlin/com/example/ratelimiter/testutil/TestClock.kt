package com.example.ratelimiter.testutil

import com.example.ratelimiter.domain.Clock
import java.util.concurrent.atomic.AtomicLong

class TestClock(startMillis: Long = 0L) : Clock {
  private val now = AtomicLong(startMillis)
  override fun nowMillis(): Long = now.get()
  fun advanceMillis(delta: Long) { now.addAndGet(delta) }
}
