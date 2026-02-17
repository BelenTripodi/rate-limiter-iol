# Notas de diseño — Rate Limiter (Spring MVC, Kotlin)

## Planteo del problema
Necesitamos un componente que decida **PERMITIR / DENEGAR** por request para prevenir abuso y proteger recursos finitos
(pool de hilos, pools downstream, CPU, etc.) manteniendo baja la latencia.

Restricciones clave:
- Corre en un servicio Spring Boot **MVC** (un hilo por request).
- Debe funcionar en despliegues **distribuidos** (múltiples instancias detrás de un balanceador).
- Debe ser correcto bajo **concurrencia** (sin “doble permiso” por carreras).
- Debe ser fácil de testear y razonar.

## Enfoque de alto nivel
1. **Control de admisión en el borde**: un `OncePerRequestFilter` de Servlet verifica límites *antes* de ejecutar los controladores.
   - Esto reduce trabajo desperdiciado y protege el pool de hilos de servlet de acumular colas grandes.
2. **Algoritmo de token bucket** para cada bucket (regla, clave):
   - Buen equilibrio para producción: permite ráfagas controladas mientras hace cumplir la tasa de largo plazo.
3. **Store de estado enchufable**:
   - `IN_MEMORY` para tests unitarios rápidos y corridas locales.
   - `REDIS` para corrección distribuida, usando **Lua** para updates atómicos.

## Capas (carpetas / paquetes)
- `web`: filtro, extracción de clave de request, respuesta de error HTTP.
- `application`: orquestación de RateLimiterEngine + PolicyResolver.
- `domain`: políticas, decisiones, value objects (lógica pura).
- `infrastructure`: stores (InMemory / Redis), abstracción de reloj, script de Redis.

Esto mantiene la lógica de dominio/aplicación testeable sin Spring.

## Por qué token bucket
Alternativas:
- Ventana fija: lo más simple pero permite ráfagas en los límites de ventana.
- Ventana deslizante: más justo pero más costoso (necesita más estado).
- **Token bucket**: soporta ráfagas con capacidad acotada; estado mínimo (tokens + timestamp).

Se eligió para un MVP limpio con comportamiento realista y buena discusión de entrevista.

## Corrección distribuida
En un despliegue multi‑instancia, contadores locales permitirían `N` por pod. Para imponer límites globales, guardamos el estado del bucket
en Redis.

Se necesita atomicidad (muchos requests pueden pegarle a distintos pods al mismo tiempo). Redis Lua ejecuta como una sola operación atómica:
- Recargar tokens según el tiempo transcurrido
- Limitar al máximo de capacidad
- Verificar y descontar el costo
- Actualizar estado + TTL

## Tiempo y deriva del reloj
Este MVP usa el reloj del servicio (`System.currentTimeMillis`) pasado a Redis.
En producción podrías preferir:
- Hora del servidor Redis (vía `TIME`) dentro del script, para evitar deriva entre nodos.

Mantenemos el MVP simple (un solo RTT por request) y marcamos la deriva como trade‑off.

## Pools de hilos: por qué importa en MVC
Spring MVC normalmente usa un modelo de **un hilo por request**. Cuando el pool de hilos de servlet se satura, los requests se encolan,
sube la latencia, y los timeouts/reintentos pueden provocar colapso por sobrecarga.

Este diseño rechaza temprano (429) y se puede combinar con un **limitador de concurrencia** (bulkhead) si hace falta:
- El rate limiting controla **requests/tiempo**.
- El limitador de concurrencia controla **trabajo en vuelo** y protege pools downstream escasos.

Incluimos los ganchos para agregar después un semáforo por ruta (Nice‑to‑have).

## Modos de falla
Si Redis está lento/no disponible, esperar sin timeouts puede consumir todos los hilos de servlet.
Mantenemos la API del store explícita para que puedas implementar estrategias **fail‑open** vs **fail‑closed**.

Este MVP por defecto es **fail‑closed** (más seguro para protección). Un sistema en producción podría elegir fail‑open para endpoints de bajo riesgo
y fail‑closed para endpoints de auth/login/caros.

## Observabilidad
Exponemos contadores vía Micrometer:
- `ratelimiter_allowed_total`
- `ratelimiter_denied_total`

En producción también medirías latencia del store, top keys y denegaciones por ruta/regla.

## Estrategia de testing
- Tests unitarios para la matemática del token bucket (reloj determinístico).
- Tests de concurrencia (requests en paralelo) para validar que nunca permitimos más que la capacidad cuando la tasa de recarga es 0.
- Tests de integración con Redis usando Testcontainers para validar que el script Lua es atómico.

## Uso de IA
Si usás herramientas de IA para generar snippets (por ej., Lua), documentá:
- qué se generó
- qué validaste (tests / razonamiento)
- qué partes complicadas revisaste (atomicidad, redondeo, unidades de tiempo)
