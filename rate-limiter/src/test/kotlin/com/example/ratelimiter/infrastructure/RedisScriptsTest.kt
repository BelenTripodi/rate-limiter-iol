package com.example.ratelimiter.infrastructure

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain

class RedisScriptsTest : FunSpec({

  test("token bucket script references expected fields") {
    RedisScripts.TOKEN_BUCKET_LUA shouldContain "tokens"
    RedisScripts.TOKEN_BUCKET_LUA shouldContain "ts"
  }
})
