package com.pgoogol.httpexchangelogger.autoconfigure;

import com.pgoogol.httpexchangelogger.factory.HttpExchangeLogEventFactory;
import com.pgoogol.httpexchangelogger.filter.HttpExchangeLoggingFilter;
import com.pgoogol.httpexchangelogger.model.HttpExchangeLogEvent;
import com.pgoogol.httpexchangelogger.resolver.EndpointLoggingModeResolver;
import com.pgoogol.httpexchangelogger.sanitizer.BodySanitizer;
import com.pgoogol.httpexchangelogger.sanitizer.HeaderSanitizer;
import com.pgoogol.httpexchangelogger.serialization.HttpExchangeLogEventJsonWriter;
import com.pgoogol.httpexchangelogger.sink.CompositeHttpExchangeLogSink;
import com.pgoogol.httpexchangelogger.sink.ConsoleHttpExchangeLogSink;
import com.pgoogol.httpexchangelogger.sink.FileHttpExchangeLogSink;
import com.pgoogol.httpexchangelogger.sink.HttpExchangeLogSink;
import com.pgoogol.httpexchangelogger.sink.NoopHttpExchangeLogSink;
import com.pgoogol.httpexchangelogger.support.ClientIpExtractor;
import com.pgoogol.httpexchangelogger.support.RequestIdProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class HttpExchangeLoggerAutoConfigurationTest {

    private final WebApplicationContextRunner webRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HttpExchangeLoggerAutoConfiguration.class));

    @Test
    void autoConfiguration_whenDefaults_registersAllExpectedBeans() {

        // given / when / then
        webRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(HttpExchangeLoggerProperties.class);
            assertThat(ctx).hasSingleBean(EndpointLoggingModeResolver.class);
            assertThat(ctx).hasSingleBean(BodySanitizer.class);
            assertThat(ctx).hasSingleBean(HeaderSanitizer.class);
            assertThat(ctx).hasSingleBean(RequestIdProvider.class);
            assertThat(ctx).hasSingleBean(ClientIpExtractor.class);
            assertThat(ctx).hasSingleBean(HttpExchangeLogEventJsonWriter.class);
            assertThat(ctx).hasSingleBean(HttpExchangeLogEventFactory.class);
            assertThat(ctx).hasSingleBean(HttpExchangeLoggingFilter.class);

            HttpExchangeLogSink sink = ctx.getBean("httpExchangeLogSink", HttpExchangeLogSink.class);
            assertThat(sink).isInstanceOf(ConsoleHttpExchangeLogSink.class);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void autoConfiguration_whenDefaults_registersFilterViaFilterRegistrationBeanWithOrder() {

        // given / when / then
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
    void autoConfiguration_whenFilterOrderPropertySet_usesConfiguredOrder() {

        // given
        webRunner.withPropertyValues("http-exchange-logger.filter-order=42")
                // when
                .run(ctx -> {
                    // then
                    @SuppressWarnings("unchecked")
                    FilterRegistrationBean<HttpExchangeLoggingFilter> registration =
                            ctx.getBean(FilterRegistrationBean.class);
                    assertThat(registration.getOrder()).isEqualTo(42);
                });
    }

    @Test
    void autoConfiguration_whenAllSinksDisabled_registersNoopSink() {

        // given
        webRunner.withPropertyValues(
                "http-exchange-logger.sink.console=false",
                "http-exchange-logger.sink.file=false"
        // when
        ).run(ctx -> {
            // then
            HttpExchangeLogSink sink = ctx.getBean("httpExchangeLogSink", HttpExchangeLogSink.class);
            assertThat(sink).isInstanceOf(NoopHttpExchangeLogSink.class);
        });
    }

    @Test
    void autoConfiguration_whenOnlyFileSinkEnabled_registersFileSink(@TempDir Path tempDir) {

        // given
        webRunner.withPropertyValues(
                "http-exchange-logger.sink.console=false",
                "http-exchange-logger.sink.file=true",
                "http-exchange-logger.sink.file-path=" + tempDir.resolve("http-exchange.log")
        // when
        ).run(ctx -> {
            // then
            HttpExchangeLogSink sink = ctx.getBean("httpExchangeLogSink", HttpExchangeLogSink.class);
            assertThat(sink).isInstanceOf(FileHttpExchangeLogSink.class);
        });
    }

    @Test
    void autoConfiguration_whenConsoleAndFileSinksEnabled_registersCompositeOverBoth(@TempDir Path tempDir) {

        // given
        webRunner.withPropertyValues(
                "http-exchange-logger.sink.file=true",
                "http-exchange-logger.sink.file-path=" + tempDir.resolve("http-exchange.log")
        // when
        ).run(ctx -> {
            // then
            HttpExchangeLogSink sink = ctx.getBean("httpExchangeLogSink", HttpExchangeLogSink.class);
            assertThat(sink).isInstanceOf(CompositeHttpExchangeLogSink.class);
            CompositeHttpExchangeLogSink composite = (CompositeHttpExchangeLogSink) sink;
            assertThat(composite.getDelegates())
                    .hasExactlyElementsOfTypes(ConsoleHttpExchangeLogSink.class, FileHttpExchangeLogSink.class);
        });
    }

    @Test
    void autoConfiguration_whenNonWebApplication_doesNotRegisterFilter() {

        // given / when / then
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(HttpExchangeLoggerAutoConfiguration.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(HttpExchangeLoggingFilter.class));
    }

    @Test
    void autoConfiguration_whenUserProvidesSink_backsOff() {

        // given
        webRunner.withUserConfiguration(CustomSinkConfig.class)
                // when
                .run(ctx -> {
                    // then
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
