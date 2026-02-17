package com.example.ratelimiter.web.identity

import com.example.ratelimiter.application.RequestIdentity
import com.example.ratelimiter.application.RequestIdentityType
import jakarta.servlet.http.HttpServletRequest

object RequestIdentityExtractor {
  private const val API_KEY_HEADER = "X-Api-Key"
  private const val X_FORWARDED_FOR = "X-Forwarded-For"

  fun apiKey(request: HttpServletRequest): RequestIdentity =
    RequestIdentity(RequestIdentityType.API_KEY, request.getHeader(API_KEY_HEADER) ?: "anonymous")

  fun ip(request: HttpServletRequest): RequestIdentity {
    val forwarded = request.getHeader(X_FORWARDED_FOR)
    val ip = forwarded?.split(",")?.firstOrNull()?.trim()
      ?: request.remoteAddr
      ?: "unknown"
    return RequestIdentity(RequestIdentityType.IP, ip)
  }
}
