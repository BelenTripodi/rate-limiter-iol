package com.example.ratelimiter.application

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RequestIdentityContextDataTest : FunSpec({

  test("data class supports copy and component access") {
    val ctx = RequestIdentityContext(
      apiKey = RequestIdentity(RequestIdentityType.API_KEY, "k"),
      ip = RequestIdentity(RequestIdentityType.IP, "1.1.1.1")
    )

    val copy = ctx.copy()
    copy shouldBe ctx
    ctx.component1() shouldBe ctx.apiKey
    ctx.component2() shouldBe ctx.ip
  }
})
