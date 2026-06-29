# Implementation Plan

Goal: make shortener-service production-ready and recruiter-impressive
via three focused feature branches.

---

## [x] feat/link-ttl — URL expiration (DONE, merged to master)

- [x] Add `expiresAt` to `Link` entity
- [x] Add `findByExpiresAtBefore` + bulk-delete query to `LinkRepository`
- [x] New `ShortenRequest` record (`url` + optional `ttlDays`) with `@Valid`/`@URL`
- [x] New `ShortenResponse` record
- [x] New `LinkExpiredException` → HTTP 410 GONE
- [x] Update `LinkDataService.getUrlFromDb` to check expiry before caching
- [x] Add `evictFromCache` to `LinkDataService`
- [x] Update `LinkService.shortenUrl` to accept TTL; re-activate expired links
- [x] Update `LinkController` to accept JSON body, return typed DTO
- [x] `GlobalExceptionHandler`: add 410, 400 (validation) handlers; extract shared builder
- [x] `LinkExpirationScheduler`: nightly cron (03:00) purges expired rows + evicts Redis
- [x] `@EnableScheduling` on `UrlShortenerApplication`
- [x] `application.properties`: `app.link.default-ttl-days`, `app.link.cleanup-cron`

---

## [x] feat/rate-limiting — Per-IP rate limiting (DONE)

Branch: `feat/rate-limiting` (from `feat/link-ttl`)

### Files to create
- [x] `config/RateLimitProperties.java` — `@ConfigurationProperties(prefix="app.rate-limit")`
- [x] `config/RateLimitInterceptor.java` — Bucket4j per-IP, separate buckets for POST/GET
- [x] `config/WebConfig.java` — registers interceptor

### Files to modify
- [x] `pom.xml` — add `bucket4j-core` dependency
- [x] `application.properties` — `app.rate-limit.shorten-limit`, `redirect-limit`, `window-seconds`

### Behaviour
- POST `/api/v1/shorten`: 20 req/min per IP (configurable)
- GET `/{code}`: 200 req/min per IP (configurable)
- Exceeding limit → HTTP 429 with JSON body `{"errorCode":"RATE_LIMIT_EXCEEDED",...}`
- Client IP resolved from `X-Forwarded-For` (first entry) or `getRemoteAddr()`
- In-memory `ConcurrentHashMap<String, Bucket>` (method:ip key)

---

## [x] feat/tests — Comprehensive test coverage (DONE)

Branch: `feat/tests` (from `feat/rate-limiting`)

### Files to create
- [x] `service/LinkServiceTest.java` — Mockito unit tests (shortenUrl, getOriginalUrl)
- [x] `service/LinkDataServiceTest.java` — Mockito unit tests (hit, miss, expired)
- [x] `controller/LinkControllerTest.java` — `@WebMvcTest` (2xx, 4xx, 410, 429)
- [x] `config/RateLimitInterceptorTest.java` — pure unit tests (under limit, over limit, X-Forwarded-For)
- [x] `resources/application-test.properties` — disable scheduling, high rate limits

### Files to modify
- [x] `UrlShortenerApplicationTests.java` — add `@MockBean` so context loads without infra

---

## Commit message convention

`feat(shortener): <summary>`  
`fix(shortener): <summary>`  
`test(shortener): <summary>`
