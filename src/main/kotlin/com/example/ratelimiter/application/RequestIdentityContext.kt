package com.example.ratelimiter.application

import com.example.ratelimiter.domain.PolicyKeyStrategy

data class RequestIdentityContext(
  val apiKey: RequestIdentity,
  val ip: RequestIdentity
) {
  fun forStrategy(strategy: PolicyKeyStrategy): RequestIdentity =
    when (strategy) {
      PolicyKeyStrategy.API_KEY -> apiKey
      PolicyKeyStrategy.IP -> ip
    }
}
