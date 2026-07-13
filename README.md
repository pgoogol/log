# http-exchange-logger-starter

Spring Boot starter, ktory automatycznie loguje requesty i response HTTP w aplikacjach Spring MVC / Servlet.

Sterowany konfiguracja, z trybami `OFF`, `BASIC`, `LIMITED`, `FULL`, regulami per endpoint, maskowaniem danych wrazliwych i limitem dlugosci body.

Kazdy exchange jest emitowany jako jedna linia JSON do loggera SLF4J `http.exchange.logger` — routing (konsola, plik, async, systemy zewnetrzne) konfiguruje sie w logging stacku aplikacji (Logback/Log4j), nie w bibliotece. Dodatkowo: sampling, czasowe nadpisywanie trybu w runtime (admin endpoint actuatora, TTL) oraz korelacje z tracingiem (traceId/spanId, atrybuty OpenTelemetry).

Pelna referencja wszystkich opcji konfiguracyjnych (z wartosciami domyslnymi, tabela i gotowymi scenariuszami): [docs/configuration.md](docs/configuration.md).

---

## Wymagania

- Spring Boot 4.x (najnowsza wersja)
- Spring Framework 7.x
- Java 21+
- Stack: Spring MVC / Servlet (jakarta.servlet)
- Jackson 3 (`tools.jackson`) — dostarczany przez Spring Boot 4, nie trzeba dodawac recznie

---

## Instalacja

```xml
<dependency>
    <groupId>com.pgoogol</groupId>
    <artifactId>http-exchange-logger-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

Po dodaniu zaleznosci auto-konfiguracja rejestruje filtr `HttpExchangeLoggingFilter` (przez `FilterRegistrationBean`) oraz wszystkie wymagane beany.

### Kolejnosc filtra

Filtr jest rejestrowany przez `FilterRegistrationBean` na wzorzec `/*`. Domyslnie dziala wczesnie (wysoki priorytet), aby objac pelny czas obslugi requestu. Kolejnosc mozna zmienic:

```yaml
http-exchange-logger:
  filter-order: 100
```

---

## Szybki start

```yaml
http-exchange-logger:
  enabled: true
  default-mode: BASIC

logging:
  level:
    http.exchange.logger: INFO
```

Jedyny warunek widocznosci logow: logger `http.exchange.logger` musi miec poziom pozwalajacy wypisac log (np. `INFO`). Ustawienie poziomu `OFF` wycisza emisje bez wylaczania filtra (request-id i atrybuty spanow dzialaja dalej).

---

## Tryby logowania

| Tryb | Zakres |
|---|---|
| `OFF` | brak logowania, filtr nie emituje eventu |
| `BASIC` | metoda, sciezka, query string, status, czas, requestId, exception |
| `LIMITED` | wszystko z BASIC + clientIp + headers + body z mniejszym limitem (`limited-max-body-length`) |
| `FULL` | wszystko z LIMITED + body do pelnego limitu (`max-body-length`) |

`FULL` zawsze stosuje maskowanie, `max-body-length` oraz pomija multiparty/binarki.

Roznica miedzy LIMITED i FULL polega na limicie dlugosci body:

- `LIMITED` tnie body do `min(limited-max-body-length, max-body-length)` (domyslnie 2000 znakow),
- `FULL` tnie body do `max-body-length` (domyslnie 10 000 znakow).

`max-body-length` zawsze pozostaje absolutnym sufitem (rowniez dla `FULL`).

---

## Reguly per endpoint

```yaml
http-exchange-logger:
  default-mode: BASIC
  endpoints:
    - pattern: /api/orders/**
      mode: FULL
    - pattern: /api/auth/**
      mode: BASIC
    - pattern: /actuator/**
      mode: OFF
    - pattern: /api/search/**
      mode: BASIC
      sample-rate: 0.1   # opcjonalnie: nadpisuje sampling.rate dla tej reguly
```

Pierwsza pasujaca regula wygrywa. Brak dopasowania - uzywany jest `default-mode`.

---

## Maskowanie

```yaml
http-exchange-logger:
  mask:
    enabled: true
    fields:
      - password
      - token
      - accessToken
      - refreshToken
      - authorization
      - cookie
      - pesel
      - email
```

Maskowanie jest case-insensitive i rekurencyjne. Wartosci pol z listy `fields` sa zamieniane na `***`. Obslugiwane formaty:

- `application/json`, `application/*+json` — pelne maskowanie rekurencyjne (obiekty i tablice),
- `application/xml`, `application/*+xml` — maskowanie tekstu wewnatrz elementow oraz wartosci atrybutow,
- `application/x-www-form-urlencoded` — maskowanie wartosci par `klucz=wartosc`,
- `text/*` — body jest logowane, ale nie ma struktury pol, wiec nie jest field-maskowane (zalecane: nie wysylac sekretow w `text/plain`).

Headery `Authorization`, `Cookie`, `Set-Cookie`, `X-Api-Key` sa maskowane **zawsze**, takze gdy `mask.enabled=false` — wylaczenie maskowania nie odslania domyslnych naglowkow z poswiadczeniami. Lista `fields` (dla body i dodatkowych naglowkow) dziala tylko przy `mask.enabled=true`.

Jezeli body jest JSON-em lub XML-em, ale nie da sie go sparsowac (np. niepoprawny lub uciety), a maskowanie pol jest wlaczone, biblioteka **nie loguje surowej tresci** — wstawia placeholder, zeby nie wyciekly sekrety (fail-closed). Maskowanie odbywa sie zawsze przed ucinaniem body. Parser XML ma wylaczone DOCTYPE i zewnetrzne encje (ochrona XXE).

---

## Limit body i pominiete typy

`max-body-length` (domyslnie 10 000 znakow) obowiazuje w kazdym trybie, takze w `FULL`. `limited-max-body-length` (domyslnie 2000) obowiazuje dodatkowo w trybie `LIMITED` — efektywny limit dla LIMITED to `min(limited-max-body-length, max-body-length)`. Po przekroczeniu limitu logowane body jest ucinane, a w evencie pojawia sie `requestBodyTruncated: true` lub `responseBodyTruncated: true`. Ustawienie `max-body-length: 0` wylacza logowanie body (pozostale metadane sa nadal logowane).

Mieszczace sie w limicie body JSON jest logowane jako zagniezdzony obiekt (`requestBody: { ... }`), a nie jako tekst. Body uciete jest logowane jako skrocony string.

Body `application/x-www-form-urlencoded` jest odtwarzane z `getParameterMap()` po wykonaniu handlera — dzieki temu nie naruszamy parsowania parametrow przez kontener, a body jest logowane (z maskowaniem).

Nie sa logowane body dla nieobslugiwanych content type:

```text
multipart/form-data
application/octet-stream
image/*
audio/*
video/*
application/pdf
application/zip
```

Obslugiwane typy:

```text
application/json
application/*+json
text/*
application/xml
application/*+xml
application/x-www-form-urlencoded
```

---

## Request ID

Kazdy event ma `requestId`:

1. jezeli request ma header `X-Request-Id`, uzywana jest jego wartosc,
2. w przeciwnym razie generowany jest UUID,
3. `X-Request-Id` jest ustawiany rowniez na response, zeby klient mogl go zobaczyc.

Mozna podmienic implementacje przez wlasnego beana `RequestIdProvider`.

---

## Format logu

Event jest serializowany jako jedna linia JSON i wysylany do loggera `http.exchange.logger` na poziomie `INFO`.

Przyklad:

```json
{"type":"http_exchange","requestId":"abc","traceId":"4bf92f35","spanId":"00f067aa","method":"POST","path":"/api/orders","status":201,"durationMs":42,"configuredMode":"FULL","effectiveMode":"FULL"}
```

`configuredMode` to tryb wynikajacy z konfiguracji statycznej, `effectiveMode` to tryb faktycznie zastosowany (moze sie roznic, gdy aktywny jest runtime override). `traceId`/`spanId` pojawiaja sie, gdy dostepny jest kontekst tracingu.

---

## Wyjscie logu — sterowanie po stronie Springa

Biblioteka ma dokladnie jedno wyjscie: logger SLF4J `http.exchange.logger`, jedna linia JSON per exchange. Nie ma zadnych wlasnych "sinkow" ani przelacznikow wyjscia — routing, pliki, rotacja, asynchronicznosc i integracje to odpowiedzialnosc logging stacku aplikacji. Dzieki temu nie duplikujemy Logbacka i wszystkie jego mozliwosci (rotacja, kompresja, retencja, appenders zewnetrzne) sa dostepne od razu.

Metryki HTTP (timer per request) rowniez sa poza zakresem biblioteki — Spring Boot z Micrometerem dostarcza je out of the box jako `http.server.requests`.

### Dedykowany plik (JSON Lines) przez Logback

```xml
<appender name="HTTP_EXCHANGE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/http-exchange.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>logs/http-exchange.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
        <maxFileSize>100MB</maxFileSize>
        <maxHistory>7</maxHistory>
    </rollingPolicy>
    <encoder><pattern>%msg%n</pattern></encoder>
</appender>

<logger name="http.exchange.logger" level="INFO" additivity="false">
    <appender-ref ref="HTTP_EXCHANGE"/>
</logger>
```

Pattern `%msg%n` zapisuje czysta linie JSON (bez prefiksu logbackowego) — plik jest wtedy poprawnym JSON Lines. `additivity="false"` wylacza dublowanie eventow do appenderow rodzica (np. konsoli).

### Emisja asynchroniczna przez Logback

```xml
<appender name="ASYNC_HTTP_EXCHANGE" class="ch.qos.logback.classic.AsyncAppender">
    <queueSize>1024</queueSize>
    <neverBlock>true</neverBlock>
    <appender-ref ref="HTTP_EXCHANGE"/>
</appender>
```

`neverBlock=true` odpowiada dawnemu zachowaniu biblioteki: watek requestu nigdy nie blokuje, nadmiarowe eventy sa odrzucane.

### Inne cele (Kafka, Loki, ELK, Slack...)

Uzyj odpowiedniego appendera (np. `logstash-logback-encoder`, appender Loki, appender Kafka) podpietego pod logger `http.exchange.logger` — biblioteka nie potrzebuje o tym wiedziec.

---

## Sampling

Ogranicza wolumen logow przy duzym ruchu. Globalny wspolczynnik (0.0 - 1.0, domyslnie 1.0 = loguj wszystko) oraz opcjonalne nadpisanie per regula endpointu:

```yaml
http-exchange-logger:
  sampling:
    rate: 0.2            # loguj ~20% exchange
  endpoints:
    - pattern: /api/orders/**
      mode: FULL
      sample-rate: 1.0   # zamowienia loguj zawsze
```

Requesty odrzucone przez sampling przechodza bez opakowywania (zero kosztu buforowania) i nadal dostaja `X-Request-Id`. Aktywny runtime override (patrz nizej) pomija sampling - skoro ktos jawnie wlaczyl logowanie, eventy nie moga znikac.

---

## Runtime overrides i admin endpoint

Tryb logowania mozna czasowo nadpisac w dzialajacej aplikacji - globalnie albo per wzorzec sciezki, z opcjonalnym TTL, po ktorym nadpisanie samo wygasa (bez restartu i bez zmiany konfiguracji).

Programowo, przez wstrzykniecie `RuntimeModeOverrideManager`:

```java
overrideManager.setPatternOverride("/api/orders/**", HttpLogMode.FULL, Duration.ofMinutes(10));
overrideManager.setGlobalOverride(HttpLogMode.OFF, null);   // do odwolania
overrideManager.clearAll();
```

Albo przez endpoint actuatora `httpexchangelogger` (wymaga zaleznosci actuatora i jawnego wystawienia):

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,httpexchangelogger
```

```text
GET    /actuator/httpexchangelogger                 -> stan konfiguracji + aktywne overridy
POST   /actuator/httpexchangelogger                 -> {"mode":"FULL","pattern":"/api/orders/**","ttlSeconds":600}
DELETE /actuator/httpexchangelogger?pattern=...     -> usun jeden override (bez pattern: usun wszystkie)
```

W evencie widac wtedy roznice: `configuredMode` pokazuje tryb z konfiguracji, `effectiveMode` tryb narzucony przez override.

**Bezpieczenstwo**: endpoint nie jest wystawiony domyslnie. Po wystawieniu ogranicz do niego dostep tak jak do reszty actuatora - siec wewnetrzna albo rola ADMIN (zgodnie z polityka bezpieczenstwa aplikacji).

### `require-ttl-for-full-logging`

```yaml
http-exchange-logger:
  require-ttl-for-full-logging: true
```

Gdy `true`, runtime override na `FULL` **musi** miec TTL - nadpisanie bez TTL jest odrzucane (`400` z endpointu, `IllegalArgumentException` z API). Chroni to przed "tymczasowym" wlaczeniem pelnego logowania, ktore zostaje na zawsze. Statycznie skonfigurowany `FULL` (w `endpoints` / `default-mode`) dziala niezaleznie od tej flagi.

---

## Tracing i OpenTelemetry

### traceId / spanId w evencie

Kazdy event moze zawierac `traceId` i `spanId`:

- domyslnie odczytywane z MDC (klucze `traceId`/`spanId` - tak propaguje je micrometer-tracing),
- gdy `micrometer-tracing` jest na classpath, uzywany jest bean `Tracer` (z fallbackiem do MDC),
- wlasna integracje mozna podpiac przez beana `TraceContextProvider`.

### Atrybuty OpenTelemetry

Gdy `opentelemetry-api` jest na classpath, biezacy span jest wzbogacany o atrybuty exchange: `http.request.method`, `http.response.status_code`, `url.path` oraz `http_exchange.request_id`, `http_exchange.mode`, `http_exchange.duration_ms` i informacje o wyjatku. Do spanow **nigdy** nie trafiaja body ani headery.

Wylaczenie:

```yaml
http-exchange-logger:
  tracing:
    otel-span-attributes: false
```

---

## Punkty rozszerzen

Wszystkie domyslne beany sa zarejestrowane z `@ConditionalOnMissingBean`. Mozna nadpisac:

- `BodySanitizer`
- `HeaderSanitizer`
- `RequestIdProvider`
- `ClientIpExtractor`
- `TraceContextProvider`
- `EndpointLoggingModeResolver`
- `ExchangeSampler`
- `RuntimeModeOverrideManager`
- `HttpExchangeLogEventFactory`
- `HttpExchangeLogEventJsonWriter` (format emitowanej linii JSON)
- `HttpExchangeSpanEnricher` (wzbogacanie spanow tracingu; domyslna implementacja OTel)

---

## Zaleznosci opcjonalne

Starter dziala bez dodatkowych zaleznosci (emisja do SLF4J, sampling, overridy programowe). Poszczegolne integracje aktywuja sie, gdy aplikacja ma na classpath:

| Funkcja | Wymagana zaleznosc |
|---|---|
| Admin endpoint `httpexchangelogger` | `spring-boot-starter-actuator` |
| `traceId`/`spanId` z beana `Tracer` | `micrometer-tracing` (bridge wg uzywanego tracera) |
| Atrybuty spanow OpenTelemetry | `opentelemetry-api` |

Bez tych zaleznosci odpowiadajace auto-konfiguracje po prostu sie nie aktywuja.

---

## Uwagi techniczne

- Response body jest przechwytywane do bufora o ograniczonym rozmiarze (ok. `4 * max-body-length` bajtow) — reszta odpowiedzi plynie bezposrednio do klienta (write-through), wiec zuzycie pamieci jest stale niezaleznie od rozmiaru odpowiedzi. Gdy odpowiedz przekroczy bufor, event dostaje `responseBodyTruncated: true`; przekroczony JSON/XML przy wlaczonym maskowaniu jest traktowany fail-closed (placeholder zamiast surowej tresci). Request body jest buforowane z gornym limitem.
- Runtime overridy sa trzymane w pamieci procesu - w srodowisku wieloinstancyjnym override ustawiony przez admin endpoint dotyczy tylko instancji, ktora obsluzyla to wywolanie.
- Event jest budowany po zakonczeniu obslugi requestu; atrybuty OpenTelemetry sa dopisywane do biezacego spana, o ile w tym momencie nadal jest aktywny.

---

## Bezpieczenstwo

1. `FULL` nadal stosuje maskowanie i `max-body-length`.
2. Multiparty i binarki nigdy nie sa logowane.
3. Tokeny, ciasteczka i hasla sa maskowane domyslnie.
4. Lista maskowanych pol jest rozszerzalna.
5. Decyzja o trybie logowania per srodowisko nalezy do aplikacji.

Biblioteka nie wprowadza parametru `allow-full-logging-in-production`. Odpowiedzialnosc za konfiguracje srodowiskowa zostaje po stronie aplikacji.
