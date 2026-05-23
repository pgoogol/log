package com.pgoogol.httpexchangelogger.autoconfigure;

import com.pgoogol.httpexchangelogger.factory.HttpExchangeLogEventFactory;
import com.pgoogol.httpexchangelogger.filter.HttpExchangeLoggingFilter;
import com.pgoogol.httpexchangelogger.model.HttpExchangeLogEvent;
import com.pgoogol.httpexchangelogger.resolver.EndpointLoggingModeResolver;
import com.pgoogol.httpexchangelogger.sanitizer.BodySanitizer;
import com.pgoogol.httpexchangelogger.sanitizer.HeaderSanitizer;
import com.pgoogol.httpexchangelogger.serialization.HttpExchangeLogEventJsonWriter;
import com.pgoogol.httpexchangelogger.sink.ConsoleHttpExchangeLogSink;
import com.pgoogol.httpexchangelogger.sink.HttpExchangeLogSink;
import com.pgoogol.httpexchangelogger.sink.NoopHttpExchangeLogSink;
import com.pgoogol.httpexchangelogger.support.ClientIpExtractor;
import com.pgoogol.httpexchangelogger.support.RequestIdProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

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
    @SuppressWarnings("unchecked")
    void registersFilterViaFilterRegistrationBeanWithOrder() {

        webRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(FilterRegistrationBean.class);
            FilterRegistrationBean<HttpExchangeLoggingFilter> registration =
                    ctx.getBean(FilterRegistrationBean.class);
            assertThat(registration.getFilter()).isInstanceOf(HttpExchangeLoggingFilter.class);
            assertThat(registration.getUrlPatterns()).contains("/*");
            assertThat(registration.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 10);
        });
    }

    @Test
    void filterOrderIsConfigurable() {

        webRunner.withPropertyValues("http-exchange-logger.filter-order=42")
                .run(ctx -> {
                    @SuppressWarnings("unchecked")
                    FilterRegistrationBean<HttpExchangeLoggingFilter> registration =
                            ctx.getBean(FilterRegistrationBean.class);
                    assertThat(registration.getOrder()).isEqualTo(42);
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

        webRunner.withUserConfiguration(CustomSinkConfig.class)
                .run(ctx -> {
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
