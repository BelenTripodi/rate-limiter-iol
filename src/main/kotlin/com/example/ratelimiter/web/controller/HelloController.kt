package com.example.ratelimiter.web.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class HelloController {
  @GetMapping("/hello")
  fun hello(): Map<String, String> = mapOf("message" to "hello")
}
