package com.example.ratelimiter.application

data class EvaluationDenied(
  val retryAfterMs: Long,
  val limit: Long,
  val refillTokensPerSecond: Double?,
  val degraded: Boolean = false
) : EvaluationResult
