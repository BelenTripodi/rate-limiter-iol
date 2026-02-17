package com.example.ratelimiter.web

import com.example.ratelimiter.application.RequestIdentityType
import com.example.ratelimiter.web.identity.RequestIdentityExtractor
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest

class RequestIdentityExtractorTest : FunSpec({

  test("apiKey defaults to anonymous when header missing") {
    val request = mockk<HttpServletRequest>()
    every { request.getHeader("X-Api-Key") } returns null

    val identity = RequestIdentityExtractor.apiKey(request)
    identity.type shouldBe RequestIdentityType.API_KEY
    identity.value shouldBe "anonymous"
  }

  test("apiKey uses header when present") {
    val request = mockk<HttpServletRequest>()
    every { request.getHeader("X-Api-Key") } returns "demo"

    val identity = RequestIdentityExtractor.apiKey(request)
    identity.type shouldBe RequestIdentityType.API_KEY
    identity.value shouldBe "demo"
  }

  test("ip uses first X-Forwarded-For value when present") {
    val request = mockk<HttpServletRequest>()
    every { request.getHeader("X-Forwarded-For") } returns "1.2.3.4, 5.6.7.8"
    every { request.remoteAddr } returns "9.9.9.9"

    val identity = RequestIdentityExtractor.ip(request)
    identity.type shouldBe RequestIdentityType.IP
    identity.value shouldBe "1.2.3.4"
  }

  test("ip falls back to remoteAddr when no X-Forwarded-For") {
    val request = mockk<HttpServletRequest>()
    every { request.getHeader("X-Forwarded-For") } returns null
    every { request.remoteAddr } returns "9.9.9.9"

    val identity = RequestIdentityExtractor.ip(request)
    identity.type shouldBe RequestIdentityType.IP
    identity.value shouldBe "9.9.9.9"
  }

  test("ip falls back to unknown when no headers") {
    val request = mockk<HttpServletRequest>()
    every { request.getHeader("X-Forwarded-For") } returns null
    every { request.remoteAddr } returns null

    val identity = RequestIdentityExtractor.ip(request)
    identity.type shouldBe RequestIdentityType.IP
    identity.value shouldBe "unknown"
  }
})
