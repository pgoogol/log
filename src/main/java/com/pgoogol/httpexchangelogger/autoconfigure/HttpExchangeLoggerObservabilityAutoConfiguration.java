package com.pgoogol.httpexchangelogger.autoconfigure;

import com.pgoogol.httpexchangelogger.sink.ObservabilityHttpExchangeLogSink;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Contributes the observability sink when Micrometer is on the classpath and
 * {@code http-exchange-logger.sink.observability=true}. Runs before the main
 * auto-configuration so the composite {@code httpExchangeLogSink} picks the
 * contributed bean up.
 */
@AutoConfiguration(before = HttpExchangeLoggerAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(MeterRegistry.class)
public class HttpExchangeLoggerObservabilityAutoConfiguration {

    @Bean(name = "observabilityHttpExchangeLogSink")
    @ConditionalOnMissingBean(name = {"observabilityHttpExchangeLogSink", "httpExchangeLogSink"})
    @ConditionalOnProperty(prefix = "http-exchange-logger.sink", name = "observability", havingValue = "true")
    public ObservabilityHttpExchangeLogSink observabilityHttpExchangeLogSink(
            ObjectProvider<MeterRegistry> meterRegistryProvider) {

        return new ObservabilityHttpExchangeLogSink(meterRegistryProvider::getIfAvailable);
    }

}
