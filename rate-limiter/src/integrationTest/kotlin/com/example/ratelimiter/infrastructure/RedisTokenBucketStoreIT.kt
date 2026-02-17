package com.example.ratelimiter.infrastructure

import com.example.ratelimiter.domain.RateLimitDecisionAllowed
import com.example.ratelimiter.domain.RateLimitDecisionDenied
import com.example.ratelimiter.domain.TokenBucketStore
import com.example.ratelimiter.testutil.TestClock
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class RedisTokenBucketStoreIT : FunSpec({

  test("redis lua script is atomic under concurrency (refill rate 0)")
    .config(enabled = DockerClientFactory.instance().isDockerAvailable) {
    val container = GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379)
    container.start()
    try {
      val host = container.host
      val port = container.getMappedPort(6379)

      val connectionFactory = LettuceConnectionFactory(host, port).apply { afterPropertiesSet() }
      val template = StringRedisTemplate(connectionFactory)

      val clock = TestClock(0)
      val store: TokenBucketStore = RedisTokenBucketStore(template, clock)

      val capacity = 10L
      val rate = 0.0
      val key = "bucket:it"

      val exec = Executors.newFixedThreadPool(16)
      try {
        val tasks = (1..100).map { Callable { store.tryConsume(key, capacity, rate, 1) } }
        val results = exec.invokeAll(tasks).map { it.get() }

        val allowed = results.count { it is RateLimitDecisionAllowed }
        val denied = results.count { it is RateLimitDecisionDenied }

        allowed shouldBe capacity.toInt()
        denied shouldBe 100 - capacity.toInt()
      } finally {
        exec.shutdownNow()
      }
    } finally {
      container.stop()
    }
  }
})
