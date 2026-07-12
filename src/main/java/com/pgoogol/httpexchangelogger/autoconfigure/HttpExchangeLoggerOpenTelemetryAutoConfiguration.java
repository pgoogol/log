package com.pgoogol.httpexchangelogger.autoconfigure;

import com.pgoogol.httpexchangelogger.sink.OpenTelemetrySpanAttributesSink;
import io.opentelemetry.api.trace.Span;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Contributes the OpenTelemetry span-attributes sink when the OTel API is on
 * the classpath. Enabled by default ({@code tracing.otel-span-attributes});
 * the sink itself is a no-op unless a valid current span exists at runtime.
 */
@AutoConfiguration(before = HttpExchangeLoggerAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(Span.class)
public class HttpExchangeLoggerOpenTelemetryAutoConfiguration {

    @Bean(name = "openTelemetryHttpExchangeLogSink")
    @ConditionalOnMissingBean(name = {"openTelemetryHttpExchangeLogSink", "httpExchangeLogSink"})
    @ConditionalOnProperty(prefix = "http-exchange-logger.tracing", name = "otel-span-attributes",
            havingValue = "true", matchIfMissing = true)
    public OpenTelemetrySpanAttributesSink openTelemetryHttpExchangeLogSink() {

        return new OpenTelemetrySpanAttributesSink();
    }

}
