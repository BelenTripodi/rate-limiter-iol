package com.example.ratelimiter.infrastructure

internal data class RedisScriptResult(
  val allowed: Boolean,
  val tokens: Double,
  val retryAfterMs: Long
)
