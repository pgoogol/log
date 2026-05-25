# Error Handling & Logging

## Exception hierarchy
Define a typed exception hierarchy per domain:
```
AppException (abstract, RuntimeException)
  ├── NotFoundException          → 404
  ├── ValidationException        → 400
  ├── ConflictException          → 409
  ├── UnauthorizedException      → 401
  ├── ForbiddenException         → 403
  └── ExternalServiceException   → 502
```
- Always include a machine-readable `errorCode` field (e.g. `ORDER_NOT_FOUND`) — not just a message string
- Never throw raw `RuntimeException` or `Exception` from business code

## Global exception handler
Use a single `@RestControllerAdvice` class — do not scatter `@ExceptionHandler` across controllers:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(NotFoundException ex) {
        log.warn("Resource not found: {}", ex.getErrorCode());
        return ErrorResponse.of(ex.getErrorCode(), ex.getMessage());
    }
}
```

## Error response format
Always return a consistent JSON body for errors:
```json
{
  "errorCode": "ORDER_NOT_FOUND",
  "message": "Order with id 123 does not exist",
  "timestamp": "2025-04-01T12:00:00Z",
  "traceId": "abc-123"
}
```
- Include `traceId` from MDC / Sleuth / Micrometer tracing
- Never expose stack traces, SQL, or internal class names in the response body

## Logging rules
- Use **SLF4J** with Logback — never `System.out.println`
- Log level guidelines:
  | Level | Use for |
  |---|---|
  | `ERROR` | Unrecoverable failures, data loss risk, external service down |
  | `WARN` | Recoverable issues, retries, fallbacks triggered |
  | `INFO` | Business events (order created, payment processed, user registered) |
  | `DEBUG` | Request/response details, method entry/exit (dev only) |
  | `TRACE` | Raw SQL, full message payloads (never in production) |
- Always use **parameterized logging** — never string concatenation:
  ```java
  // WRONG
  log.info("Processing order " + orderId + " for user " + userId);
  // CORRECT
  log.info("Processing order {} for user {}", orderId, userId);
  ```
- Add `traceId` and `userId` to MDC at the start of each request (use a filter or interceptor)
- Never log inside a tight loop — use summary logs after the loop

## Transactional error handling
- Mark a method `@Transactional(rollbackFor = Exception.class)` if it must rollback on checked exceptions
- Default Spring behavior rolls back on `RuntimeException` only — be explicit when needed
- Do not swallow exceptions inside `@Transactional` methods — re-throw or wrap them

## WebFlux / reactive error handling
- Use `.onErrorMap()` to convert low-level exceptions to domain exceptions
- Use `.onErrorResume()` for fallback logic
- Never block inside a reactive chain — use `Mono.fromCallable()` with a bounded scheduler for blocking calls:
  ```java
  Mono.fromCallable(() -> blockingRepository.findById(id))
      .subscribeOn(Schedulers.boundedElastic());
  ```

## Retries & circuit breakers
- Use **Resilience4j** — not `@Retryable` from Spring Retry for new code
- Configure retry only for **idempotent** operations (GET, PUT, DELETE, Kafka consumer)
- Never retry on `4xx` errors — only on `5xx` and timeouts
- Add a circuit breaker on every external HTTP call and Kafka producer
