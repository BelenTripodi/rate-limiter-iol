package com.example.ratelimiter.web

data class ErrorResponse(
  val code: ErrorCode,
  val message: String,
  val retryAfterMs: Long,
  val retryAfterSeconds: Long
)
