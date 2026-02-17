package com.example.ratelimiter.infrastructure.config

import com.example.ratelimiter.application.PolicyResolver
import com.example.ratelimiter.application.RateLimiterEngine
import com.example.ratelimiter.config.RateLimiterMode
import com.example.ratelimiter.config.RateLimiterProperties
import com.example.ratelimiter.domain.Clock
import com.example.ratelimiter.domain.TokenBucketStore
import com.example.ratelimiter.infrastructure.InMemoryTokenBucketStore
import com.example.ratelimiter.infrastructure.RedisTokenBucketStore
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate

@Configuration
class BeansConfig {

  @Bean
  fun clock(): Clock = Clock.SystemClock

  @Bean
  fun tokenBucketStore(
    props: RateLimiterProperties,
    clock: Clock,
    redisTemplateProvider: ObjectProvider<StringRedisTemplate>
  ): TokenBucketStore =
    when (props.mode) {
      RateLimiterMode.IN_MEMORY -> InMemoryTokenBucketStore(clock)
      RateLimiterMode.REDIS -> {
        val redisTemplate = redisTemplateProvider.getIfAvailable()
          ?: error("ratelimiter.mode=REDIS requiere StringRedisTemplate configurado")
        RedisTokenBucketStore(redisTemplate, clock)
      }
    }

  @Bean
  fun policyResolver(props: RateLimiterProperties): PolicyResolver =
    PolicyResolver(props)

  @Bean
  fun rateLimiterEngine(
    store: TokenBucketStore,
    resolver: PolicyResolver,
    props: RateLimiterProperties
  ): RateLimiterEngine =
    RateLimiterEngine(store, resolver, props.failStrategy)
}
