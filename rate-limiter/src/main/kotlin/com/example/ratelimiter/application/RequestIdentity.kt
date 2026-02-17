package com.example.ratelimiter.application

data class RequestIdentity(
  val type: RequestIdentityType,
  val value: String
)
