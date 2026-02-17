package com.example.ratelimiter.application

import com.example.ratelimiter.config.RateLimiterKeyStrategy
import com.example.ratelimiter.config.RateLimiterProperties
import com.example.ratelimiter.domain.Policy
import com.example.ratelimiter.domain.PolicyKeyStrategy
import org.springframework.util.AntPathMatcher

class PolicyResolver(private val props: RateLimiterProperties) {
  private val matcher = AntPathMatcher()

  fun resolve(path: String): List<Policy> =
    props.rules
      .filter { matcher.match(it.pathPattern, path) }
      .map {
        Policy(
          name = it.name,
          pathPattern = it.pathPattern,
          keyStrategy = when (it.keyStrategy) {
            RateLimiterKeyStrategy.API_KEY -> PolicyKeyStrategy.API_KEY
            RateLimiterKeyStrategy.IP -> PolicyKeyStrategy.IP
          },
          capacity = it.capacity,
          refillTokensPerSecond = it.refillTokensPerSecond,
          cost = it.cost
        )
      }
}
