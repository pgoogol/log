package com.pgoogol.httpexchangelogger.autoconfigure;

import com.pgoogol.httpexchangelogger.factory.HttpExchangeLogEventFactory;
import com.pgoogol.httpexchangelogger.filter.HttpExchangeLoggingFilter;
import com.pgoogol.httpexchangelogger.resolver.EndpointLoggingModeResolver;
import com.pgoogol.httpexchangelogger.sanitizer.BodySanitizer;
import com.pgoogol.httpexchangelogger.sanitizer.DefaultHeaderSanitizer;
import com.pgoogol.httpexchangelogger.sanitizer.HeaderSanitizer;
import com.pgoogol.httpexchangelogger.sanitizer.JsonBodySanitizer;
import com.pgoogol.httpexchangelogger.sanitizer.SensitiveValueMasker;
import com.pgoogol.httpexchangelogger.serialization.HttpExchangeLogEventJsonWriter;
import com.pgoogol.httpexchangelogger.sink.CompositeHttpExchangeLogSink;
import com.pgoogol.httpexchangelogger.sink.ConsoleHttpExchangeLogSink;
import com.pgoogol.httpexchangelogger.sink.HttpExchangeLogSink;
import com.pgoogol.httpexchangelogger.sink.NoopHttpExchangeLogSink;
import com.pgoogol.httpexchangelogger.support.ClientIpExtractor;
import com.pgoogol.httpexchangelogger.support.DefaultClientIpExtractor;
import com.pgoogol.httpexchangelogger.support.DefaultRequestIdProvider;
import com.pgoogol.httpexchangelogger.support.RequestIdProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(HttpExchangeLoggerProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(OncePerRequestFilter.class)
public class HttpExchangeLoggerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EndpointLoggingModeResolver endpointLoggingModeResolver(HttpExchangeLoggerProperties properties) {
        return new EndpointLoggingModeResolver(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public SensitiveValueMasker sensitiveValueMasker(HttpExchangeLoggerProperties properties) {
        return new SensitiveValueMasker(properties.getMask().getFields());
    }

    @Bean
    @ConditionalOnMissingBean
    public HeaderSanitizer headerSanitizer(SensitiveValueMasker masker,
                                           HttpExchangeLoggerProperties properties) {
        return new DefaultHeaderSanitizer(masker, properties.getMask());
    }

    @Bean
    @ConditionalOnMissingBean
    public BodySanitizer bodySanitizer(SensitiveValueMasker masker,
                                       HttpExchangeLoggerProperties properties,
                                       ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new JsonBodySanitizer(resolveObjectMapper(objectMapperProvider), masker, properties.getMask());
    }

    @Bean
    @ConditionalOnMissingBean
    public RequestIdProvider requestIdProvider() {
        return new DefaultRequestIdProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientIpExtractor clientIpExtractor() {
        return new DefaultClientIpExtractor();
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpExchangeLogEventJsonWriter httpExchangeLogEventJsonWriter(ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new HttpExchangeLogEventJsonWriter(resolveObjectMapper(objectMapperProvider));
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpExchangeLogEventFactory httpExchangeLogEventFactory(HttpExchangeLoggerProperties properties,
                                                                    HeaderSanitizer headerSanitizer,
                                                                    BodySanitizer bodySanitizer,
                                                                    ClientIpExtractor clientIpExtractor,
                                                                    ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new HttpExchangeLogEventFactory(properties, headerSanitizer, bodySanitizer,
                clientIpExtractor, resolveObjectMapper(objectMapperProvider));
    }

    private static ObjectMapper resolveObjectMapper(ObjectProvider<ObjectMapper> objectMapperProvider) {
        return objectMapperProvider.getIfAvailable(() -> JsonMapper.builder().build());
    }

    @Bean(name = "httpExchangeLogSink")
    @ConditionalOnMissingBean(name = "httpExchangeLogSink")
    public HttpExchangeLogSink httpExchangeLogSink(HttpExchangeLoggerProperties properties,
                                                   HttpExchangeLogEventJsonWriter jsonWriter) {
        List<HttpExchangeLogSink> sinks = new ArrayList<>();
        if (properties.getSink().isConsole()) {
            sinks.add(new ConsoleHttpExchangeLogSink(jsonWriter));
        }
        if (sinks.isEmpty()) {
            return new NoopHttpExchangeLogSink();
        }
        if (sinks.size() == 1) {
            return sinks.get(0);
        }
        return new CompositeHttpExchangeLogSink(sinks);
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpExchangeLoggingFilter httpExchangeLoggingFilter(HttpExchangeLoggerProperties properties,
                                                               EndpointLoggingModeResolver modeResolver,
                                                               HttpExchangeLogEventFactory eventFactory,
                                                               HttpExchangeLogSink sink,
                                                               RequestIdProvider requestIdProvider) {
        return new HttpExchangeLoggingFilter(properties, modeResolver, eventFactory, sink, requestIdProvider);
    }

    @Bean
    @ConditionalOnMissingBean(name = "httpExchangeLoggingFilterRegistration")
    public FilterRegistrationBean<HttpExchangeLoggingFilter> httpExchangeLoggingFilterRegistration(
            HttpExchangeLoggingFilter filter,
            HttpExchangeLoggerProperties properties) {
        FilterRegistrationBean<HttpExchangeLoggingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setName("httpExchangeLoggingFilter");
        registration.addUrlPatterns("/*");
        registration.setOrder(properties.getFilterOrder());
        return registration;
    }
}
