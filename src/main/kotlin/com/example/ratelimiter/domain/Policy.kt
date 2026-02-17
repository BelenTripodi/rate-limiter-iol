package com.example.ratelimiter.domain

data class Policy(
  val name: String,
  val pathPattern: String,
  val keyStrategy: PolicyKeyStrategy,
  val capacity: Long,
  val refillTokensPerSecond: Double,
  val cost: Long
)
