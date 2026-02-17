package com.example.ratelimiter.domain

interface TokenBucketStore {
  fun tryConsume(
    key: String,
    capacity: Long,
    refillTokensPerSecond: Double,
    cost: Long
  ): RateLimitDecision
}
