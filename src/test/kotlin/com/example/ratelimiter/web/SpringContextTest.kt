package com.example.ratelimiter.web

import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class SpringContextTest : FunSpec() {
  override fun extensions() = listOf(SpringExtension)

  init {
    test("context loads") { }
  }
}
