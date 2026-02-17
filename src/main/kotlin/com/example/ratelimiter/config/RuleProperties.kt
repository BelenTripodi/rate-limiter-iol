package com.example.ratelimiter.config

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class RuleProperties(
  @field:NotBlank val name: String,
  @field:NotBlank val pathPattern: String,
  val keyStrategy: RateLimiterKeyStrategy = RateLimiterKeyStrategy.API_KEY,
  @field:Min(1) val capacity: Long,
  val refillTokensPerSecond: Double,
  @field:Min(1) val cost: Long = 1
)
