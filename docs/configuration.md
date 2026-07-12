# Konfiguracja — pelna referencja

Wszystkie wlasciwosci startera znajduja sie pod prefixem `http-exchange-logger`
(klasa `HttpExchangeLoggerProperties`). Ponizej pelny YAML z wartosciami
domyslnymi i opisami, tabela referencyjna oraz gotowe konfiguracje dla
typowych scenariuszy.

---

## Pelny YAML (wszystkie opcje + wartosci domyslne)

```yaml
http-exchange-logger:
  # Globalny wylacznik calej biblioteki. false = filtr nie jest rejestrowany.
  enabled: true

  # Tryb logowania uzywany, gdy zadna regula z `endpoints` nie pasuje.
  # Dozwolone: OFF | BASIC | LIMITED | FULL
  default-mode: BASIC

  # Gdy true, runtime override na FULL (admin endpoint / API) MUSI miec TTL.
  # Override bez TTL jest odrzucany (400 z endpointu, IllegalArgumentException z API).
  # Nie dotyczy statycznie skonfigurowanego FULL.
  require-ttl-for-full-logging: false

  # Absolutny sufit dlugosci logowanego body (znaki) — obowiazuje w KAZDYM trybie,
  # takze FULL. 0 = body w ogole nie jest logowane (metadane nadal tak).
  max-body-length: 10000

  # Limit body w trybie LIMITED. Efektywny limit dla LIMITED to
  # min(limited-max-body-length, max-body-length).
  limited-max-body-length: 2000

  # Kolejnosc filtra servletowego. Domyslnie bardzo wysoki priorytet
  # (Ordered.HIGHEST_PRECEDENCE + 10), zeby objac pelny czas obslugi requestu.
  filter-order: -2147483638

  # ---------- Sinki (miejsca docelowe eventow) ----------
  sink:
    # SLF4J logger "http.exchange.logger", poziom INFO, jedna linia JSON.
    console: true
    # JSON Lines dopisywane do pliku; przy bledzie degraduje sie do no-op.
    file: false
    # Sciezka file sinka; katalogi nadrzedne tworzone automatycznie przy starcie.
    file-path: logs/http-exchange.log

  # ---------- Zakres danych w evencie ----------
  include:
    # Czy logowac naglowki (dotyczy trybow LIMITED/FULL).
    headers: true
    # Czy logowac query string.
    query-string: true
    # Czy logowac IP klienta (dotyczy trybow LIMITED/FULL).
    client-ip: true

  # ---------- Maskowanie danych wrazliwych ----------
  mask:
    # Wlacza maskowanie pol z listy `fields` w body i naglowkach.
    # UWAGA: naglowki Authorization, Cookie, Set-Cookie, X-Api-Key sa maskowane
    # ZAWSZE, nawet gdy enabled=false.
    enabled: true
    # Nazwy pol maskowanych na "***" — case-insensitive, rekurencyjnie
    # (JSON, XML, x-www-form-urlencoded). Podanie wlasnej listy ZASTEPUJE domyslna.
    fields:
      - password
      - token
      - accessToken
      - refreshToken
      - authorization
      - cookie
      - pesel
      - email

  # ---------- Sampling ----------
  sampling:
    # Ulamek exchange'ow, ktore sa logowane (0.0–1.0). 1.0 = loguj wszystko.
    # Requesty odrzucone przez sampling nie sa buforowane (zero narzutu)
    # i nadal dostaja X-Request-Id. Runtime override pomija sampling.
    rate: 1.0

  # ---------- Emisja asynchroniczna ----------
  async:
    # true = eventy trafiaja do sinkow przez osobny watek i ograniczona kolejke.
    enabled: false
    # Pojemnosc kolejki; gdy pelna, event jest ODRZUCANY (watek requestu nigdy
    # nie blokuje), odrzucenia zliczane (WARN co 100).
    queue-capacity: 1000
    # Ile ms shutdown czeka na oproznienie kolejki do sinkow.
    shutdown-timeout-ms: 2000

  # ---------- Tracing ----------
  tracing:
    # Gdy opentelemetry-api jest na classpath: dopisuj do biezacego spana atrybuty
    # http.request.method, http.response.status_code, url.path,
    # http_exchange.request_id / .mode / .duration_ms i info o wyjatku.
    # Body i naglowki NIGDY nie trafiaja do spanow.
    otel-span-attributes: true

  # ---------- Reguly per endpoint ----------
  # Pierwsza pasujaca regula wygrywa; brak dopasowania = default-mode.
  endpoints:
    - pattern: /api/orders/**   # wzorzec Ant-style
      mode: FULL                # OFF | BASIC | LIMITED | FULL
      sample-rate: 1.0          # opcjonalnie: nadpisuje sampling.rate dla tej reguly
    - pattern: /actuator/**
      mode: OFF
```

Konfiguracja **poza** prefixem biblioteki, potrzebna w dwoch przypadkach:

```yaml
# 1. Warunek konieczny dla console sinka — logger musi przepuszczac INFO:
logging:
  level:
    http.exchange.logger: INFO

# 2. Admin endpoint (wymaga spring-boot-starter-actuator i jawnego wystawienia):
management:
  endpoints:
    web:
      exposure:
        include: health,httpexchangelogger
```

---

## Tabela referencyjna

| Wlasciwosc | Typ | Domyslnie | Opis |
|---|---|---|---|
| `enabled` | boolean | `true` | Globalny wylacznik startera |
| `default-mode` | enum | `BASIC` | Tryb, gdy zadna regula endpointu nie pasuje |
| `require-ttl-for-full-logging` | boolean | `false` | Wymusza TTL na runtime override'ach FULL |
| `max-body-length` | int | `10000` | Absolutny limit znakow body (0 = bez body) |
| `limited-max-body-length` | int | `2000` | Limit body w trybie LIMITED (przycinany do `max-body-length`) |
| `filter-order` | int | `HIGHEST_PRECEDENCE + 10` | Kolejnosc filtra servletowego |
| `sink.console` | boolean | `true` | Sink SLF4J (`http.exchange.logger`) |
| `sink.file` | boolean | `false` | Sink plikowy JSON Lines |
| `sink.file-path` | String | `logs/http-exchange.log` | Plik docelowy file sinka |
| `include.headers` | boolean | `true` | Czy logowac naglowki |
| `include.query-string` | boolean | `true` | Czy logowac query string |
| `include.client-ip` | boolean | `true` | Czy logowac IP klienta |
| `mask.enabled` | boolean | `true` | Maskowanie pol z listy `fields` |
| `mask.fields` | List | 8 pol (patrz wyzej) | Maskowane pola; wlasna lista zastepuje domyslna |
| `sampling.rate` | double | `1.0` | Globalny wspolczynnik samplingu (0.0–1.0) |
| `async.enabled` | boolean | `false` | Asynchroniczna emisja eventow |
| `async.queue-capacity` | int | `1000` | Pojemnosc ograniczonej kolejki |
| `async.shutdown-timeout-ms` | long | `2000` | Czas na oproznienie kolejki przy shutdownie |
| `tracing.otel-span-attributes` | boolean | `true` | Atrybuty `http_exchange.*` na spanach OTel |
| `endpoints[].pattern` | String | — | Wzorzec sciezki (Ant-style) |
| `endpoints[].mode` | enum | — | Tryb dla wzorca |
| `endpoints[].sample-rate` | Double | `null` | Nadpisanie samplingu per regula |

---

## Gotowe konfiguracje dla typowych scenariuszy

### Dev — pelna widocznosc

```yaml
http-exchange-logger:
  default-mode: FULL
  endpoints:
    - pattern: /actuator/**
      mode: OFF

logging:
  level:
    http.exchange.logger: INFO
```

### Produkcja — bezpieczny minimalizm + awaryjne wlaczanie FULL

Metryki HTTP (timer per request) zapewnia sam Spring Boot z Micrometerem
(`http.server.requests`) — biblioteka ich nie duplikuje.

```yaml
http-exchange-logger:
  default-mode: BASIC
  require-ttl-for-full-logging: true    # FULL z override'u zawsze wygasnie
  sink:
    console: true
  endpoints:
    - pattern: /actuator/**
      mode: OFF
    - pattern: /api/auth/**
      mode: BASIC                       # nigdy body na endpointach auth

management:
  endpoints:
    web:
      exposure:
        include: health,httpexchangelogger   # dostep ograniczyc do roli ADMIN / sieci wewnetrznej
```

### Duzy ruch — sampling + async + file sink

```yaml
http-exchange-logger:
  default-mode: LIMITED
  sampling:
    rate: 0.1                     # ~10% ruchu
  async:
    enabled: true
    queue-capacity: 5000
  sink:
    console: false
    file: true
    file-path: /var/log/app/http-exchange.log
  endpoints:
    - pattern: /api/payments/**
      mode: FULL
      sample-rate: 1.0            # platnosci zawsze w 100%
    - pattern: /api/health/**
      mode: OFF
```

### Debugowanie jednego endpointu bez restartu

Programowo:

```java
overrideManager.setPatternOverride("/api/orders/**", HttpLogMode.FULL, Duration.ofMinutes(10));
```

Przez actuator:

```text
POST /actuator/httpexchangelogger
{"mode":"FULL","pattern":"/api/orders/**","ttlSeconds":600}
```

### Wylaczenie logowania body przy zachowaniu metadanych

```yaml
http-exchange-logger:
  default-mode: FULL
  max-body-length: 0        # body nie jest logowane, reszta eventu tak
```

---

## Pulapki konfiguracyjne

1. **`mask.fields` zastepuje liste domyslna** — podajac wlasna liste trzeba
   przekopiowac pola domyslne i dopisac wlasne, inaczej np. `password`
   przestanie byc maskowane w body.
2. **Console sink wymaga dwoch warunkow naraz**: `sink.console: true` **i**
   poziom `INFO` (lub nizszy) dla loggera `http.exchange.logger`. Brak
   ktoregokolwiek = brak logow w konsoli.
3. **`max-body-length` jest sufitem absolutnym** — ustawienie
   `limited-max-body-length` powyzej `max-body-length` nie zwiekszy limitu
   dla LIMITED.
4. **Admin endpoint nie jest wystawiony domyslnie** — wymaga actuatora
   i jawnego dodania `httpexchangelogger` do `exposure.include`.
5. **Runtime overridy sa per instancja** — w srodowisku wieloinstancyjnym
   override ustawiony przez admin endpoint dziala tylko na instancji,
   ktora obsluzyla wywolanie.
