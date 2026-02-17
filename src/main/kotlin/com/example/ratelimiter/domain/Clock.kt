package com.example.ratelimiter.domain

fun interface Clock {
  fun nowMillis(): Long

  companion object {
    val SystemClock: Clock = Clock { System.currentTimeMillis() }
  }
}
