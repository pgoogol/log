package com.yourcompany.httpexchangelogger.autoconfigure;

import com.yourcompany.httpexchangelogger.factory.HttpExchangeLogEventFactory;
import com.yourcompany.httpexchangelogger.filter.HttpExchangeLoggingFilter;
import com.yourcompany.httpexchangelogger.model.HttpExchangeLogEvent;
import com.yourcompany.httpexchangelogger.resolver.EndpointLoggingModeResolver;
import com.yourcompany.httpexchangelogger.sanitizer.BodySanitizer;
import com.yourcompany.httpexchangelogger.sanitizer.HeaderSanitizer;
import com.yourcompany.httpexchangelogger.serialization.HttpExchangeLogEventJsonWriter;
import com.yourcompany.httpexchangelogger.sink.ConsoleHttpExchangeLogSink;
import com.yourcompany.httpexchangelogger.sink.HttpExchangeLogSink;
import com.yourcompany.httpexchangelogger.sink.NoopHttpExchangeLogSink;
import com.yourcompany.httpexchangelogger.support.ClientIpExtractor;
import com.yourcompany.httpexchangelogger.support.RequestIdProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class HttpExchangeLoggerAutoConfigurationTest {

    private final WebApplicationContextRunner webRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HttpExchangeLoggerAutoConfiguration.class));

    @Test
    void registersAllExpectedBeansByDefault() {
        webRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(HttpExchangeLoggerProperties.class);
            assertThat(ctx).hasSingleBean(EndpointLoggingModeResolver.class);
            assertThat(ctx).hasSingleBean(BodySanitizer.class);
            assertThat(ctx).hasSingleBean(HeaderSanitizer.class);
            assertThat(ctx).hasSingleBean(RequestIdProvider.class);
            assertThat(ctx).hasSingleBean(ClientIpExtractor.class);
            assertThat(ctx).hasSingleBean(HttpExchangeLogEventJsonWriter.class);
            assertThat(ctx).hasSingleBean(HttpExchangeLogEventFactory.class);
            assertThat(ctx).hasSingleBean(HttpExchangeLogSink.class);
            assertThat(ctx).hasSingleBean(HttpExchangeLoggingFilter.class);

            HttpExchangeLogSink sink = ctx.getBean(HttpExchangeLogSink.class);
            assertThat(sink).isInstanceOf(ConsoleHttpExchangeLogSink.class);
        });
    }

    @Test
    void usesNoopSinkWhenConsoleDisabled() {
        webRunner.withPropertyValues(
                "http-exchange-logger.sink.console=false",
                "http-exchange-logger.sink.file=true",
                "http-exchange-logger.sink.observability=true"
        ).run(ctx -> {
            HttpExchangeLogSink sink = ctx.getBean(HttpExchangeLogSink.class);
            assertThat(sink).isInstanceOf(NoopHttpExchangeLogSink.class);
        });
    }

    @Test
    void doesNotConfigureFilterWhenNonWebApplication() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(HttpExchangeLoggerAutoConfiguration.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(HttpExchangeLoggingFilter.class));
    }

    @Test
    void backsOffWhenUserProvidesSink() {
        webRunner.withUserConfiguration(CustomSinkConfig.class).run(ctx -> {
            HttpExchangeLogSink sink = ctx.getBean(HttpExchangeLogSink.class);
            assertThat(sink).isInstanceOf(CustomSink.class);
        });
    }

    @Configuration
    static class CustomSinkConfig {
        @Bean(name = "httpExchangeLogSink")
        HttpExchangeLogSink customSink() {
            return new CustomSink();
        }
    }

    static class CustomSink implements HttpExchangeLogSink {
        @Override
        public void log(HttpExchangeLogEvent event) {
        }
    }
}
