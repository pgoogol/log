package com.yourcompany.httpexchangelogger.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourcompany.httpexchangelogger.factory.HttpExchangeLogEventFactory;
import com.yourcompany.httpexchangelogger.filter.HttpExchangeLoggingFilter;
import com.yourcompany.httpexchangelogger.resolver.EndpointLoggingModeResolver;
import com.yourcompany.httpexchangelogger.sanitizer.BodySanitizer;
import com.yourcompany.httpexchangelogger.sanitizer.DefaultHeaderSanitizer;
import com.yourcompany.httpexchangelogger.sanitizer.HeaderSanitizer;
import com.yourcompany.httpexchangelogger.sanitizer.JsonBodySanitizer;
import com.yourcompany.httpexchangelogger.sanitizer.SensitiveValueMasker;
import com.yourcompany.httpexchangelogger.serialization.HttpExchangeLogEventJsonWriter;
import com.yourcompany.httpexchangelogger.sink.CompositeHttpExchangeLogSink;
import com.yourcompany.httpexchangelogger.sink.ConsoleHttpExchangeLogSink;
import com.yourcompany.httpexchangelogger.sink.HttpExchangeLogSink;
import com.yourcompany.httpexchangelogger.sink.NoopHttpExchangeLogSink;
import com.yourcompany.httpexchangelogger.support.ClientIpExtractor;
import com.yourcompany.httpexchangelogger.support.DefaultClientIpExtractor;
import com.yourcompany.httpexchangelogger.support.DefaultRequestIdProvider;
import com.yourcompany.httpexchangelogger.support.RequestIdProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.OncePerRequestFilter;

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
        ObjectMapper mapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new JsonBodySanitizer(mapper, masker, properties.getMask());
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
        ObjectMapper mapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new HttpExchangeLogEventJsonWriter(mapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpExchangeLogEventFactory httpExchangeLogEventFactory(HttpExchangeLoggerProperties properties,
                                                                    HeaderSanitizer headerSanitizer,
                                                                    BodySanitizer bodySanitizer,
                                                                    ClientIpExtractor clientIpExtractor) {
        return new HttpExchangeLogEventFactory(properties, headerSanitizer, bodySanitizer, clientIpExtractor);
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
}
