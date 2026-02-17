package com.example.ratelimiter.infrastructure

import java.util.concurrent.locks.ReentrantLock

internal data class BucketState(
  var tokens: Double,
  var lastRefillMs: Long,
  val lock: ReentrantLock = ReentrantLock()
)
