package com.pgoogol.httpexchangelogger.sink;

import com.pgoogol.httpexchangelogger.model.HttpExchangeLogEvent;
import io.opentelemetry.api.trace.Span;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Enriches the current OpenTelemetry span with HTTP exchange attributes.
 * Standard semantic-convention keys are set alongside namespaced
 * {@code http_exchange.*} ones carrying library-specific metadata.
 *
 * <p>When there is no valid current span (no OTel SDK configured, or the
 * request is not traced) the sink is a no-op. Attribute values never include
 * bodies or headers — only low-risk exchange metadata.
 */
public class OpenTelemetrySpanAttributesSink implements HttpExchangeLogSink {

    private final Supplier<Span> currentSpanSupplier;

    public OpenTelemetrySpanAttributesSink() {

        this(Span::current);
    }

    public OpenTelemetrySpanAttributesSink(Supplier<Span> currentSpanSupplier) {

        this.currentSpanSupplier = currentSpanSupplier;
    }

    @Override
    public void log(HttpExchangeLogEvent event) {

        if (Objects.isNull(event)) {

            return;
        }
        Span span = currentSpanSupplier.get();
        if (Objects.isNull(span) || !span.getSpanContext().isValid()) {

            return;
        }
        span.setAttribute("http.request.method", Objects.toString(event.getMethod(), "UNKNOWN"));
        span.setAttribute("http.response.status_code", event.getStatus());
        span.setAttribute("url.path", Objects.toString(event.getPath(), ""));
        span.setAttribute("http_exchange.request_id", Objects.toString(event.getRequestId(), ""));
        span.setAttribute("http_exchange.mode", Objects.toString(event.getEffectiveMode(), "UNKNOWN"));
        span.setAttribute("http_exchange.duration_ms", event.getDurationMs());
        if (Objects.nonNull(event.getExceptionClass())) {

            span.setAttribute("http_exchange.exception.type", event.getExceptionClass());
            span.setAttribute("http_exchange.exception.message",
                    Objects.toString(event.getExceptionMessage(), ""));
        }
    }

}
