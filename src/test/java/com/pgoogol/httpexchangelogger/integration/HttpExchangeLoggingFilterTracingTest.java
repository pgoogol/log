package com.pgoogol.httpexchangelogger.integration;

import com.pgoogol.httpexchangelogger.filter.HttpExchangeLoggingFilter;
import com.pgoogol.httpexchangelogger.model.HttpExchangeLogEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = HttpExchangeLoggingFilterTracingTest.TestApp.class)
class HttpExchangeLoggingFilterTracingTest {

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    RecordingHttpExchangeLogSink sink;

    @Autowired
    HttpExchangeLoggingFilter filter;

    private MockMvc mockMvc() {

        return MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(filter)
                .build();
    }

    @AfterEach
    void cleanUp() {

        MDC.clear();
        sink.clear();
    }

    @Test
    void doFilter_whenMdcCarriesTraceContext_eventContainsTraceAndSpanIds() throws Exception {

        // given the tracing setup propagated ids into the MDC (as micrometer-tracing does)
        MDC.put("traceId", "trace-123");
        MDC.put("spanId", "span-456");

        // when
        mockMvc().perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"p_1\"}"))
                .andExpect(status().isOk());

        // then
        HttpExchangeLogEvent event = sink.last();
        assertThat(event.getTraceId()).isEqualTo("trace-123");
        assertThat(event.getSpanId()).isEqualTo("span-456");
    }

    @Test
    void doFilter_whenNoTraceContext_eventHasNoTraceIds() throws Exception {

        // given an empty MDC

        // when
        mockMvc().perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"p_1\"}"))
                .andExpect(status().isOk());

        // then
        HttpExchangeLogEvent event = sink.last();
        assertThat(event.getTraceId()).isNull();
        assertThat(event.getSpanId()).isNull();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {

        @Bean
        TestController testController() {

            return new TestController();
        }

        @Bean(name = "httpExchangeLogSink")
        RecordingHttpExchangeLogSink recordingSink() {

            return new RecordingHttpExchangeLogSink();
        }

    }

}
