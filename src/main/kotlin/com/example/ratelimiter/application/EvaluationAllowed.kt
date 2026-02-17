package com.example.ratelimiter.application

data class EvaluationAllowed(
  val remainingTokens: Long,
  val limit: Long,
  val refillTokensPerSecond: Double?,
  val degraded: Boolean = false
) : EvaluationResult
