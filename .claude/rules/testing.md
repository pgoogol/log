# Testing

## Tooling
- **Unit tests**: JUnit 5 + Mockito + AssertJ
- **Integration tests**: `@SpringBootTest` + Testcontainers
- **Web layer tests**: `@WebMvcTest` (MVC) or `@WebFluxTest` (WebFlux)
- **Repository tests**: `@DataJpaTest` + Testcontainers (never use H2 for JPA tests)
- **Contract tests**: Spring Cloud Contract or Pact
- **Kafka**: `EmbeddedKafkaBroker` for unit, Testcontainers for integration

## Naming convention
```
[methodUnderTest]_[whatItDoes]_[expectedResult]

createOrder_whenItemOutOfStock_throwsInsufficientStockException
findById_whenUserExists_returnsUserResponse
```

## Test structure
Structure every test body in explicit **given / when / then** sections, separated
by `// given`, `// when`, `// then` comments (or blank lines):
```java
@Test
void createOrder_whenItemOutOfStock_throwsInsufficientStockException() {
    
    // given
    var request = OrderFixtures.outOfStockRequest();

    // when
    var thrown = catchThrowable(() -> service.createOrder(request));

    // then
    assertThat(thrown).isInstanceOf(InsufficientStockException.class);
}
```

## Unit test rules
- One `@Test` = one assertion concept (can use `assertAll` for related fields)
- Use `@ExtendWith(MockitoExtension.class)` — never `@SpringBootTest` for pure unit tests
- Mock only direct dependencies, not transitive ones
- Use `ArgumentCaptor` to verify what was passed to mocks, not just that they were called
- Never test private methods directly — test behavior through the public API
- Avoid `Thread.sleep()` in tests — use `Awaitility` for async assertions

## Integration test rules
- Use `@Testcontainers` + real database image (e.g. `postgres:16-alpine`)
- Share one container across the test suite with `@Container` + `static` field
- Use `@Sql("/test-data/orders.sql")` for test fixtures — not hardcoded inserts in test methods
- Reset state between tests with `@Transactional` (rollback) or `@Sql(executionPhase = AFTER_TEST_METHOD)`
- Test the full HTTP stack with `MockMvc` or `WebTestClient` — not by calling service methods directly

## Coverage targets
- Service layer: **≥ 80%** line coverage
- Critical paths (payment, auth, data mutation): **100%** branch coverage
- Do not chase coverage numbers — untested edge cases matter more than the percentage

## Test data builders
Use the **Builder pattern** or **Object Mother** for test fixtures — never repeat `new Order(...)` with 10 args across tests:
```java
// Object Mother
public class OrderFixtures {
    public static Order pendingOrder() {
        return Order.builder()
            .id(UUID.randomUUID())
            .status(OrderStatus.PENDING)
            .items(List.of(OrderItemFixtures.oneItem()))
            .build();
    }
}
```

## What NOT to test
- Spring framework internals (auto-configuration, bean wiring)
- Simple getters/setters on entities (unless they contain logic)
- Trivial one-liners that are obvious from the code
