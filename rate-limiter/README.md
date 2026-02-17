# Rate Limiter

## Problem & Requirements (estilo Alex Xu)

**Problema**  
Diseñar un rate limiter que decida **ALLOW / DENY** por request para proteger el servicio contra abuso y mantener la latencia baja.

**Requisitos funcionales**
- Limitar requests por una **clave** (API key o IP).
- Definir **reglas** por ruta (path pattern) con capacidad y refill.
- Responder **429** cuando se excede el límite.
- Soportar modo **IN_MEMORY** y **REDIS**.

**Requisitos no funcionales**
- **Baja latencia** (decidir antes del controller).
- **Correctitud bajo concurrencia** (sin “double allow”).
- **Compatibilidad distribuida** (varias instancias).
- **Testabilidad** y estructura clara por capas.

**Constraints / Assumptions**
- Servicio **Spring Boot MVC** (thread‑per‑request).
- Múltiples instancias detrás de un **load balancer**.
- Decisiones **determinísticas** y **rápidas**.
- Redis es opcional, pero necesario para límites **globales**.
- Priorizar **correctitud y tests** sobre optimizaciones prematuras.

## Overview

Implementación simple de **rate limiting** en **Spring Boot MVC** y Kotlin.

Incluye:
- Control de admisión temprano (Servlet `Filter`)
- Algoritmo **token bucket**
- Correctitud **distribuida** con **Redis + Lua** (updates atómicos)
- Capas claras (web / application / domain / infrastructure)
- Tests con Kotest + MockK + Testcontainers

## Quick start

### 1) Levantar Redis (solo si usás modo REDIS)
```bash
docker compose up -d redis
```

### 2) Generar Gradle wrapper (recomendado)
Si no tenés `./gradlew` en el repo:
```bash
gradle wrapper
```

### 3) Correr la app
```bash
./gradlew bootRun
```

### 4) Probar
La config por defecto rate‑limita por `X-Api-Key`.

```bash
curl -i http://localhost:8080/hello -H "X-Api-Key: demo"
```

Hacelo varias veces rápido:
```bash
for i in {1..20}; do curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/hello -H "X-Api-Key: demo"; done
```

## Cómo usarlo

### Configuración mínima (obligatoria)
En `src/main/resources/application.yml` necesitás definir:

- `ratelimiter.mode`: `IN_MEMORY` o `REDIS`
- `ratelimiter.rules`: lista de reglas (al menos una)

Ejemplo:
```yaml
ratelimiter:
  mode: IN_MEMORY
  failStrategy: FAIL_CLOSED
  rules:
    - name: apiKey-global
      pathPattern: "/**"
      keyStrategy: API_KEY
      capacity: 10
      refillTokensPerSecond: 5.0
      cost: 1
```

### Si usás Redis
- Asegurate de tener Redis levantado y accesible.
- Configurá `spring.data.redis.host` y `spring.data.redis.port` si no son los defaults.

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

## Docs
- Diseño y trade‑offs: **DESIGN.md**

## Notas del challenge
- Este repo implementa el problema **Rate Limiter** del libro de Alex Xu.
- Se prioriza: **build verde**, **tests fuertes**, y **código claro**.
