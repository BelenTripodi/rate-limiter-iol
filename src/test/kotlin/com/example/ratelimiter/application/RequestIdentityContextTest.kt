package com.example.ratelimiter.application

import com.example.ratelimiter.domain.PolicyKeyStrategy
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RequestIdentityContextTest : FunSpec({

  test("selects api key identity for API_KEY strategy") {
    val ctx = RequestIdentityContext(
      apiKey = RequestIdentity(RequestIdentityType.API_KEY, "k"),
      ip = RequestIdentity(RequestIdentityType.IP, "1.1.1.1")
    )

    ctx.forStrategy(PolicyKeyStrategy.API_KEY).value shouldBe "k"
  }

  test("selects ip identity for IP strategy") {
    val ctx = RequestIdentityContext(
      apiKey = RequestIdentity(RequestIdentityType.API_KEY, "k"),
      ip = RequestIdentity(RequestIdentityType.IP, "1.1.1.1")
    )

    ctx.forStrategy(PolicyKeyStrategy.IP).value shouldBe "1.1.1.1"
  }
})
