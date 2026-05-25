# Code Style & Naming Conventions

## Java version features
- Use **records** for DTOs and value objects — never plain POJOs with getters/setters for data carriers
- Use **sealed classes** for domain result types (e.g. `Success | Failure | NotFound`)
- Use **pattern matching** (`instanceof`, `switch`) instead of cascaded if-else
- Use **virtual threads** (`Executors.newVirtualThreadPerTaskExecutor()`) for blocking I/O tasks
- **Do not use `var`** — always declare the explicit type for local variables
- Use text blocks for multi-line SQL, JSON, and HTML strings

## Naming
| Element | Convention | Example |
|---|---|---|
| Class | PascalCase, noun | `OrderService`, `PaymentGateway` |
| Interface | PascalCase, noun or adjective | `Auditable`, `OrderRepository` |
| Method | camelCase, verb | `findById`, `calculateTotal` |
| Constant | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Package | lowercase, singular | `com.company.app.order` |
| DTO | Suffix with `Request` / `Response` | `CreateOrderRequest`, `OrderResponse` |
| Exception | Suffix with `Exception` | `OrderNotFoundException` |
| Config class | Suffix with `Config` | `SecurityConfig`, `KafkaConfig` |
| Entity | Plain noun, no suffix | `Order`, `User` |

## Code structure rules
- Maximum method length: **30 lines** — extract if longer
- Maximum class length: **300 lines** — split by responsibility
- No `static` utility classes — use Spring beans or standalone functions in Kotlin-style companion objects
- No `null` returns from public methods — use `Optional<T>` or throw a typed exception
- Prefer `List.of()`, `Map.of()`, `Set.of()` for immutable collections
- Always use `@NonNull` / `@Nullable` (from `org.springframework.lang`) on method parameters and return types

## Loops & chaining
- Avoid plain `for` loops (both indexed `for (int i …)` and enhanced `for (x : xs)`) — prefer `Stream` / `forEach` / declarative constructs, **unless** a plain loop is genuinely cheaper in time or complexity (then a `for` is fine, and say why)
- Avoid chained calls like `a().b().c()` — unless using a Builder, `Optional`, `Stream`, or Mockito API

## Spring-specific
- Use constructor injection only — never `@Autowired` on fields
- Mark service classes `@Transactional` at class level only when ALL methods need it; otherwise annotate individual methods
- Use `@Value` only for simple scalar configs; use `@ConfigurationProperties` for groups of related properties
- Never use `@Autowired` on constructors — Spring injects automatically when there is only one constructor
- Keep `@RestController` thin: no business logic, only input validation + delegation to service

## Comparisons & null checks
- Use `Objects.equals(a, b)` instead of `a.equals(b)` or `a == b` for object equality — null-safe
- Use `Objects.isNull(x)` / `Objects.nonNull(x)` instead of `x == null` / `x != null`
- Use `Objects.requireNonNull(x, "message")` for guard clauses at the top of methods
- Use `Objects.requireNonNullElse(x, default)` instead of ternary null checks
- Use `Objects.toString(x, "fallback")` instead of `x != null ? x.toString() : "fallback"`
- Exception: `== null` is acceptable inside `equals()` overrides and null-check chain starts

```java
// WRONG
if (order.getStatus() == null || order.getStatus().equals(other.getStatus())) { ... }
if (user != null) return user.getName();

// CORRECT
if (Objects.isNull(order.getStatus()) || Objects.equals(order.getStatus(), other.getStatus())) { ... }
return Objects.toString(user, "unknown");
```

## Collections — CollectionUtils
- Use `CollectionUtils.isEmpty(col)` / `CollectionUtils.isNotEmpty(col)` instead of `col == null || col.isEmpty()`
- Use `CollectionUtils.emptyIfNull(col)` instead of ternary null-to-empty-list guards
- Use `CollectionUtils.containsAny(col, candidates)` instead of manual stream + anyMatch for simple membership checks
- Use `CollectionUtils.intersection(a, b)` / `union(a, b)` / `subtract(a, b)` instead of manual set operations
- Use Apache Commons `CollectionUtils` (`org.apache.commons.collections4`) — not Spring's limited variant unless already in scope

```java
// WRONG
if (orders == null || orders.isEmpty()) { ... }
List<String> tags = user.getTags() != null ? user.getTags() : Collections.emptyList();
boolean hasRole = roles.stream().anyMatch(allowed::contains);

// CORRECT
if (CollectionUtils.isEmpty(orders)) { ... }
List<String> tags = CollectionUtils.emptyIfNull(user.getTags());
boolean hasRole = CollectionUtils.containsAny(roles, allowed);
```

## Formatting
- 4-space indentation (no tabs)
- Opening brace on the same line
- Leave one blank line after an opening brace `{` that starts a class or method body
- Enforce with Checkstyle or Spotless (`./mvnw spotless:check`)