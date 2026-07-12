package com.pgoogol.httpexchangelogger.autoconfigure;

import com.pgoogol.httpexchangelogger.sink.CompositeHttpExchangeLogSink;
import com.pgoogol.httpexchangelogger.sink.HttpExchangeLogSink;
import com.pgoogol.httpexchangelogger.sink.ObservabilityHttpExchangeLogSink;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class HttpExchangeLoggerObservabilityAutoConfigurationTest {

    private final WebApplicationContextRunner webRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    HttpExchangeLoggerObservabilityAutoConfiguration.class,
                    HttpExchangeLoggerAutoConfiguration.class));

    @Test
    void observabilityAutoConfiguration_whenEnabled_contributesSinkToComposite() {

        // given
        webRunner.withPropertyValues("http-exchange-logger.sink.observability=true"
        // when
        ).run(ctx -> {
            // then
            assertThat(ctx).hasSingleBean(ObservabilityHttpExchangeLogSink.class);
            HttpExchangeLogSink primary = ctx.getBean("httpExchangeLogSink", HttpExchangeLogSink.class);
            assertThat(primary).isInstanceOf(CompositeHttpExchangeLogSink.class);
            CompositeHttpExchangeLogSink composite = (CompositeHttpExchangeLogSink) primary;
            assertThat(composite.getDelegates())
                    .anySatisfy(delegate -> assertThat(delegate).isInstanceOf(ObservabilityHttpExchangeLogSink.class));
        });
    }

    @Test
    void observabilityAutoConfiguration_whenDisabledByDefault_registersNoObservabilitySink() {

        // given default properties / when
        webRunner.run(ctx ->
                // then
                assertThat(ctx).doesNotHaveBean(ObservabilityHttpExchangeLogSink.class));
    }

    @Test
    void observabilityAutoConfiguration_whenMicrometerMissingFromClasspath_backsOff() {

        // given
        webRunner.withClassLoader(new FilteredClassLoader(MeterRegistry.class))
                .withPropertyValues("http-exchange-logger.sink.observability=true")
                // when
                .run(ctx ->
                        // then
                        assertThat(ctx).doesNotHaveBean("observabilityHttpExchangeLogSink"));
    }

}
