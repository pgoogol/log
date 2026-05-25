# Database & Performance

## Flyway migrations
- Every schema change goes through a Flyway migration — never modify the schema manually or via `ddl-auto`
- Set `spring.jpa.hibernate.ddl-auto=validate` in production, `none` when Flyway manages the schema
- Naming: `V{version}__{description}.sql` — e.g. `V20240401_01__add_orders_table.sql`
- Migrations must be **idempotent** where possible (use `IF NOT EXISTS`, `IF EXISTS`)
- Never modify an existing migration that has been applied — create a new one
- Test migrations in CI with Testcontainers before merging

## JPA / Hibernate rules
- Always set `fetch = FetchType.LAZY` on `@OneToMany` and `@ManyToMany` — never EAGER
- Use `@EntityGraph` or JOIN FETCH in queries when you know you need related data — avoid N+1
- Enable SQL logging in dev to catch N+1 queries:
  ```yaml
  spring.jpa.show-sql: true
  spring.jpa.properties.hibernate.format_sql: true
  logging.level.org.hibernate.SQL: DEBUG
  logging.level.org.hibernate.type.descriptor.sql: TRACE
  ```
- Use **Hypersistence Utils** `@QueryHints` with `HINT_PASS_DISTINCT_THROUGH` for JOIN FETCH deduplication
- Prefer **projections** (interfaces or records) over full entity fetches for read-only queries:
  ```java
  interface OrderSummary {
      UUID getId();
      String getStatus();
      BigDecimal getTotal();
  }
  List<OrderSummary> findAllByUserId(UUID userId);
  ```
- Use `@Version` on entities that are updated concurrently — enables optimistic locking

## Query rules
- Use **Spring Data derived queries** for simple lookups (1–2 conditions)
- Use **JPQL with `@Query`** for joins and projections
- Use **native SQL with `@Query(nativeQuery = true)`** only for complex aggregations or DB-specific features
- Use **`@Modifying` + `@Transactional`** for bulk updates/deletes — never load entities just to delete them
- Always add database indexes for: foreign keys, columns in `WHERE` / `ORDER BY` / `JOIN` clauses, unique constraints

## Connection pool (HikariCP)
Tune for your workload — defaults are often wrong:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20        # start here, tune with load testing
      minimum-idle: 5
      connection-timeout: 3000     # 3s — fail fast
      idle-timeout: 600000         # 10min
      max-lifetime: 1800000        # 30min — less than DB connection timeout
      leak-detection-threshold: 5000
```

## Pagination
- Never return unbounded lists from the API — always paginate with `Pageable`
- Default page size: **20**, max: **100** — validate and cap in the controller
- Use **keyset pagination** (cursor-based) for large datasets instead of offset pagination

## Kafka
- Always set `enable.auto.commit=false` — commit offsets manually after successful processing
- Use **dead-letter topics** (DLT) for messages that fail after max retries — do not silently discard
- Consumer group IDs must be unique per service and environment
- Idempotent producers: set `enable.idempotence=true` and `acks=all`
- Schema registry (Avro/Protobuf) for inter-service message contracts — never raw JSON without a schema

## Caching
- Use Spring Cache abstraction (`@Cacheable`, `@CacheEvict`) — do not hardcode Redis calls in services
- Cache read-heavy, rarely-changing data: reference data, config, user roles
- Always set a TTL — never cache indefinitely
- Cache keys must include all parameters that affect the result
- Test cache eviction explicitly — missing eviction is a common bug
