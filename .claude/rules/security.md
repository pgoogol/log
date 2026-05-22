# Security

## General
- Never disable CSRF unless explicitly building a stateless REST API protected by JWT/OAuth2
- Never log sensitive data: passwords, tokens, card numbers, personal identifiers (PESEL, IBAN)
- Never store secrets in code or `application.properties` — use environment variables or a vault (HashiCorp Vault, AWS Secrets Manager)
- Always sanitize user input before passing to SQL, file paths, or external commands

## Spring Security configuration
- Use the **SecurityFilterChain bean** approach (Spring Security 6) — never extend `WebSecurityConfigurerAdapter`
- Explicitly define which endpoints are public; default to deny-all:
```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/public/**").permitAll()
    .anyRequest().authenticated()
);
```
- Enable method-level security with `@EnableMethodSecurity` and use `@PreAuthorize` on service methods, not just controllers
- Use `@PreAuthorize("hasRole('ADMIN')")` not `hasAuthority('ROLE_ADMIN')` — keep roles consistent

## JWT / OAuth2
- Token expiry: access token max **15 minutes**, refresh token max **7 days**
- Always validate: signature, expiry (`exp`), issuer (`iss`), audience (`aud`)
- Store refresh tokens server-side (DB or Redis) to enable revocation
- Use `spring-security-oauth2-resource-server` — do not hand-roll JWT parsing
- Never put sensitive claims (passwords, full credit card numbers) inside JWT payload

## Passwords
- Always hash with **BCrypt** (strength ≥ 12) — never MD5, SHA1, or plain SHA256
- Use `PasswordEncoder` bean — never instantiate `BCryptPasswordEncoder` inline

## HTTP headers
Enable security headers in config:
```java
http.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
    .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
    .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
);
```

## Input validation
- Use `@Valid` / `@Validated` on all controller method parameters that accept request bodies
- Define validation constraints on the DTO, not in service code
- Return `400 Bad Request` for validation failures — never `500`
- Use `@Pattern`, `@Size`, `@NotBlank` — avoid writing custom validators for things Bean Validation covers

## Actuator
- Never expose `/actuator` publicly in production
- Restrict to internal network or require ADMIN role:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```
