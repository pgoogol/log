# http-exchange-logger-starter

Spring Boot starter, ktory automatycznie loguje requesty i response HTTP w aplikacjach Spring MVC / Servlet.

Sterowany konfiguracja, z trybami `OFF`, `BASIC`, `LIMITED`, `FULL`, regulami per endpoint, maskowaniem danych wrazliwych i limitem dlugosci body.

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
  sink:
    console: true

logging:
  level:
    http.exchange.logger: INFO
```

Aby logi pojawily sie w konsoli, musza byc spelnione dwa warunki:

1. `http-exchange-logger.sink.console=true`
2. logger `http.exchange.logger` ma poziom pozwalajacy wypisac log (np. `INFO`)

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
{"type":"http_exchange","requestId":"abc","method":"POST","path":"/api/orders","status":201,"durationMs":42,"configuredMode":"FULL","effectiveMode":"FULL"}
```

---

## Sinki

W MVP dziala wylacznie console sink. Parametry `sink.file` i `sink.observability` istnieja w kontrakcie konfiguracji, ale w MVP nie maja efektu.

Konfiguracja:

```yaml
http-exchange-logger:
  sink:
    console: true       # dziala
    file: true          # ignorowane w MVP
    observability: true # ignorowane w MVP
```

Aby zarejestrowac wlasny sink, zdefiniuj beana o nazwie `httpExchangeLogSink`:

```java
@Bean(name = "httpExchangeLogSink")
public HttpExchangeLogSink myCustomSink() {
    return event -> {
        // wlasna logika
    };
}
```

Alternatywnie mozna laczyc kilka sinkow w `CompositeHttpExchangeLogSink`.

---

## Punkty rozszerzen

Wszystkie domyslne beany sa zarejestrowane z `@ConditionalOnMissingBean`. Mozna nadpisac:

- `HttpExchangeLogSink` (bean o nazwie `httpExchangeLogSink`)
- `BodySanitizer`
- `HeaderSanitizer`
- `RequestIdProvider`
- `ClientIpExtractor`
- `EndpointLoggingModeResolver`
- `HttpExchangeLogEventFactory`
- `HttpExchangeLogEventJsonWriter`

---

## Parametr `require-ttl-for-full-logging`

```yaml
http-exchange-logger:
  require-ttl-for-full-logging: false
```

W MVP parametr jest tylko mapowany z konfiguracji. Aplikacja moze go ustawic, ale biblioteka go ignoruje - `FULL` dziala zgodnie z konfiguracja endpointow i `default-mode`. Parametr jest zarezerwowany pod przyszly mechanizm dynamicznego, czasowego wlaczania logowania (TTL).

---

## Ograniczenia MVP

Poza pierwsza wersja zostaje:

- file sink,
- observability sink (Micrometer, OpenTelemetry),
- admin endpoint do czasowej zmiany trybu logowania,
- dynamiczne TTL,
- asynchroniczne logowanie,
- sampling.

Uwaga techniczna: response body jest buforowane w pamieci przez `ContentCachingResponseWrapper` (standard Spring) zanim zostanie ograniczone przez `max-body-length`. Dla bardzo duzych odpowiedzi oznacza to chwilowe zuzycie pamieci proporcjonalne do rozmiaru odpowiedzi. Request body jest buforowane z gornym limitem.

---

## Bezpieczenstwo

1. `FULL` nadal stosuje maskowanie i `max-body-length`.
2. Multiparty i binarki nigdy nie sa logowane.
3. Tokeny, ciasteczka i hasla sa maskowane domyslnie.
4. Lista maskowanych pol jest rozszerzalna.
5. Decyzja o trybie logowania per srodowisko nalezy do aplikacji.

Biblioteka nie wprowadza parametru `allow-full-logging-in-production`. Odpowiedzialnosc za konfiguracje srodowiskowa zostaje po stronie aplikacji.
