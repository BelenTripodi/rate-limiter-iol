# Rate Limiter

Servicio de ejemplo que aplica **rate limiting por request** (ALLOW/DENY) con algoritmo **Token Bucket**, ejecutándose **antes del controller** mediante un `OncePerRequestFilter`.

* Modos: `IN_MEMORY` (single instance) y `REDIS` (distributed)
* Redis: consistencia distribuida con **Lua** (update atómico del bucket)
* Tests: Kotest + MockK + Testcontainers (Redis)

> Decisiones de diseño y trade-offs: ver **DESIGN.md**

---

## Quick start

### 1) (Opcional) Levantar Redis

Solo si vas a usar `ratelimiter.mode=REDIS`:

```bash
docker compose up -d redis
```

### 2) Correr la app

```bash
./gradlew bootRun
```

### 3) Probar

Por defecto rate-limita por `X-Api-Key`:

```bash
curl -i http://localhost:8080/hello -H "X-Api-Key: demo"
```

Stress rápido:

```bash
for i in {1..20}; do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/hello -H "X-Api-Key: demo"
done
```

---

## Configuración

### Modo de store

```yaml
ratelimiter:
  mode: IN_MEMORY   # o REDIS
  failStrategy: FAIL_CLOSED  # o FAIL_OPEN
```

### Rules (policies)

Cada rule define:

* `pathPattern` (Ant style)
* `keyStrategy` (`API_KEY` o `IP`)
* `capacity`, `refillTokensPerSecond`, `cost`

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

### Redis (si aplica)

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

---

## Respuestas HTTP

* **200/2xx**: request permitida
* **429 Too Many Requests**: límite excedido
  Incluye `Retry-After` cuando corresponde y headers `RateLimit-*` (ver implementación del filter).

---

## Estructura

* `web/`: filter + extracción de identidad (apiKey/IP)
* `application/`: engine + resolver de rules
* `domain/`: modelos + interfaces (store, clock)
* `infrastructure/`: InMemory store + Redis store (Lua)


