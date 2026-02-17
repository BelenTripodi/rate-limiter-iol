package com.example.ratelimiter.web.filter

import com.example.ratelimiter.application.EvaluationAllowed
import com.example.ratelimiter.application.EvaluationDenied
import com.example.ratelimiter.application.RateLimiterEngine
import com.example.ratelimiter.application.RequestIdentityContext
import com.example.ratelimiter.web.ErrorCode
import com.example.ratelimiter.web.ErrorResponse
import com.example.ratelimiter.web.identity.RequestIdentityExtractor
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.slf4j.LoggerFactory
import kotlin.math.ceil

@Component
class RateLimiterFilter(
  private val engine: RateLimiterEngine,
  private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {
  private val log = LoggerFactory.getLogger(javaClass)

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain
  ) {
    val identities = RequestIdentityContext(
      apiKey = RequestIdentityExtractor.apiKey(request),
      ip = RequestIdentityExtractor.ip(request)
    )

    try {
      when (val result = engine.evaluate(request, identities)) {
        is EvaluationAllowed ->
          applyAllowed(request, response, result) { filterChain.doFilter(request, response) }
        is EvaluationDenied ->
          applyDenied(request, response, result)
      }
    } catch (e: Exception) {
      log.error("Rate limiting filter failed", e)
      if (!response.isCommitted) {
        filterChain.doFilter(request, response)
      }
    }
  }

  private fun applyAllowed(
    request: HttpServletRequest,
    response: HttpServletResponse,
    result: EvaluationAllowed,
    onContinue: () -> Unit
  ) {
    response.takeIf { !it.isCommitted }?.let { servletResponse ->
      setRateLimitHeaders(
        response = servletResponse,
        limit = result.limit,
        remaining = result.remainingTokens,
        resetSeconds = null
      )
      setDebugHeadersIfAllowed(
        request = request,
        response = servletResponse,
        isAllowed = true,
        degraded = result.degraded,
        remaining = result.remainingTokens,
        refillTokensPerSecond = result.refillTokensPerSecond
      )
    }
    onContinue()
  }

  private fun applyDenied(
    request: HttpServletRequest,
    response: HttpServletResponse,
    result: EvaluationDenied
  ) {
    response.takeIf { !it.isCommitted }?.let { servletResponse ->
      val retryAfterSeconds = maxOf(1L, ceil(result.retryAfterMs / 1000.0).toLong())
      servletResponse.status = 429
      servletResponse.contentType = MediaType.APPLICATION_JSON_VALUE
      servletResponse.characterEncoding = "utf-8"
      servletResponse.setHeader("Retry-After", retryAfterSeconds.toString())
      setRateLimitHeaders(
        response = servletResponse,
        limit = result.limit,
        remaining = 0,
        resetSeconds = retryAfterSeconds
      )
      setDebugHeadersIfAllowed(
        request = request,
        response = servletResponse,
        isAllowed = false,
        degraded = result.degraded,
        remaining = 0,
        refillTokensPerSecond = result.refillTokensPerSecond
      )

      val body = ErrorResponse(
        code = ErrorCode.RATE_LIMIT_EXCEEDED,
        message = "Too many requests",
        retryAfterMs = result.retryAfterMs,
        retryAfterSeconds = retryAfterSeconds
      )
      servletResponse.writer.write(objectMapper.writeValueAsString(body))
      servletResponse.writer.flush()
    }
  }

  private fun setRateLimitHeaders(
    response: HttpServletResponse,
    limit: Long,
    remaining: Long?,
    resetSeconds: Long?
  ) {
    limit.takeIf { it != Long.MAX_VALUE }?.let {
      response.setHeader("RateLimit-Limit", it.toString())
    }
    remaining?.let { response.setHeader("RateLimit-Remaining", it.toString()) }
    resetSeconds?.let { response.setHeader("RateLimit-Reset", it.toString()) }
  }

  private fun setDebugHeadersIfAllowed(
    request: HttpServletRequest,
    response: HttpServletResponse,
    isAllowed: Boolean,
    degraded: Boolean,
    remaining: Long,
    refillTokensPerSecond: Double?
  ) {
    if (!isDebugAllowed(request)) return
    response.setHeader("RateLimit-Debug-Allowed", isAllowed.toString())
    response.setHeader("RateLimit-Debug-Degraded", degraded.toString())
    response.setHeader("RateLimit-Debug-Remaining", remaining.toString())
    refillTokensPerSecond?.let {
      response.setHeader("RateLimit-Debug-Refill-Per-Second", it.toString())
    }
  }

  private fun isDebugAllowed(request: HttpServletRequest): Boolean {
    val client = request.getHeader("Client")?.trim().orEmpty()
    val debug = request.getHeader("Debug")?.equals("true", ignoreCase = true) == true
    return debug && client.isNotBlank()
  }

}
