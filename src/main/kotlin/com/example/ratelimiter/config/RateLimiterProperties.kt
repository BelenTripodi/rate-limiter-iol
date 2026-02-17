package com.example.ratelimiter.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "ratelimiter")
data class RateLimiterProperties(
  val mode: RateLimiterMode = RateLimiterMode.IN_MEMORY,
  val failStrategy: RateLimiterFailStrategy = RateLimiterFailStrategy.FAIL_CLOSED,
  @field:jakarta.validation.Valid val rules: List<@jakarta.validation.Valid RuleProperties> = emptyList()
)
