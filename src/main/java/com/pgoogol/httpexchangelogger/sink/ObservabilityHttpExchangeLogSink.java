package com.pgoogol.httpexchangelogger.sink;

import com.pgoogol.httpexchangelogger.model.HttpExchangeLogEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Publishes each exchange as a Micrometer {@code http.exchange} timer with
 * low-cardinality tags (method, status, outcome, mode, exception). The request
 * path is deliberately not a tag — it would create unbounded cardinality.
 *
 * <p>The registry is resolved lazily so this sink does not depend on the
 * auto-configuration order of the application's {@link MeterRegistry}; when no
 * registry exists the sink stays a no-op.
 */
public class ObservabilityHttpExchangeLogSink implements HttpExchangeLogSink {

    public static final String TIMER_NAME = "http.exchange";

    private static final Logger LOG = LoggerFactory.getLogger(ObservabilityHttpExchangeLogSink.class);

    private final Supplier<MeterRegistry> registrySupplier;

    private volatile MeterRegistry resolvedRegistry;
    private volatile boolean lookupDone;

    public ObservabilityHttpExchangeLogSink(Supplier<MeterRegistry> registrySupplier) {

        this.registrySupplier = registrySupplier;
    }

    @Override
    public void log(HttpExchangeLogEvent event) {

        if (Objects.isNull(event)) {

            return;
        }
        MeterRegistry registry = registry();
        if (Objects.isNull(registry)) {

            return;
        }
        Timer.builder(TIMER_NAME)
                .description("HTTP exchange duration as observed by http-exchange-logger")
                .tag("method", Objects.toString(event.getMethod(), "UNKNOWN"))
                .tag("status", String.valueOf(event.getStatus()))
                .tag("outcome", outcome(event.getStatus()))
                .tag("mode", Objects.toString(event.getEffectiveMode(), "UNKNOWN"))
                .tag("exception", exceptionTag(event.getExceptionClass()))
                .register(registry)
                .record(event.getDurationMs(), TimeUnit.MILLISECONDS);
    }

    private MeterRegistry registry() {

        if (!lookupDone) {

            resolvedRegistry = registrySupplier.get();
            lookupDone = true;
            if (Objects.isNull(resolvedRegistry)) {

                LOG.debug("No MeterRegistry available; observability sink stays inactive");
            }
        }
        return resolvedRegistry;
    }

    private String outcome(int status) {

        if (status >= 100 && status < 200) {

            return "INFORMATIONAL";
        }
        if (status < 300) {

            return "SUCCESS";
        }
        if (status < 400) {

            return "REDIRECTION";
        }
        if (status < 500) {

            return "CLIENT_ERROR";
        }
        if (status < 600) {

            return "SERVER_ERROR";
        }
        return "UNKNOWN";
    }

    private String exceptionTag(String exceptionClass) {

        if (Objects.isNull(exceptionClass) || exceptionClass.isBlank()) {

            return "none";
        }
        int lastDot = exceptionClass.lastIndexOf('.');
        if (lastDot < 0 || lastDot == exceptionClass.length() - 1) {

            return exceptionClass;
        }
        return exceptionClass.substring(lastDot + 1);
    }

}
