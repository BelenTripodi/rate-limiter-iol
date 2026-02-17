package com.example.ratelimiter.domain

data class RateLimitDecisionDenied(val retryAfterMs: Long) : RateLimitDecision
