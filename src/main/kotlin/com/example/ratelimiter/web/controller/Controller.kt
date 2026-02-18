package com.example.ratelimiter.web.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class Controller {
  @GetMapping("/hello")
  fun hello(): Map<String, String> = mapOf("message" to "hello")

  @GetMapping("/v1/search")
  fun search(@RequestParam("q", required = false) query: String?): Map<String?, String> =
      mapOf(query to "hello")
}
