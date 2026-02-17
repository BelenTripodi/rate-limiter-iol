package com.example.ratelimiter.domain

data class RateLimitDecisionAllowed(val remainingTokens: Long) : RateLimitDecision
