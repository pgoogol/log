package com.pgoogol.httpexchangelogger.integration;

import com.pgoogol.httpexchangelogger.filter.HttpExchangeLoggingFilter;
import com.pgoogol.httpexchangelogger.model.HttpExchangeLogEvent;
import com.pgoogol.httpexchangelogger.model.HttpLogMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = HttpExchangeLoggingFilterIntegrationTest.TestApp.class)
@TestPropertySource(properties = {
        "http-exchange-logger.enabled=true",
        "http-exchange-logger.default-mode=LIMITED",
        "http-exchange-logger.max-body-length=200",
        "http-exchange-logger.endpoints[0].pattern=/api/orders",
        "http-exchange-logger.endpoints[0].mode=FULL",
        "http-exchange-logger.endpoints[1].pattern=/api/orders/**",
        "http-exchange-logger.endpoints[1].mode=FULL",
        "http-exchange-logger.endpoints[2].pattern=/api/auth/**",
        "http-exchange-logger.endpoints[2].mode=BASIC",
        "http-exchange-logger.endpoints[3].pattern=/actuator/**",
        "http-exchange-logger.endpoints[3].mode=OFF",
        "http-exchange-logger.endpoints[4].pattern=/api/boom",
        "http-exchange-logger.endpoints[4].mode=FULL",
        "http-exchange-logger.endpoints[5].pattern=/api/binary",
        "http-exchange-logger.endpoints[5].mode=FULL"
})
class HttpExchangeLoggingFilterIntegrationTest {

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
    void clearSink() {

        sink.clear();
    }

    @Test
    void basicModeOnlyLogsMetadata() throws Exception {

        MvcResult result = mockMvc().perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("fake-token");

        HttpExchangeLogEvent event = sink.last();
        assertThat(event.getMethod()).isEqualTo("POST");
        assertThat(event.getPath()).isEqualTo("/api/auth/login");
        assertThat(event.getStatus()).isEqualTo(200);
        assertThat(event.getConfiguredMode()).isEqualTo(HttpLogMode.BASIC);
        assertThat(event.getRequestBody()).isNull();
        assertThat(event.getResponseBody()).isNull();
        assertThat(event.getRequestHeaders()).isNull();
        assertThat(event.getResponseHeaders()).isNull();
        assertThat(event.getRequestId()).isNotBlank();
    }

    @Test
    void offModeSkipsLogging() throws Exception {

        mockMvc().perform(get("/actuator/health"))
                .andExpect(status().isOk());

        assertThat(sink.getEvents()).isEmpty();
    }

    @Test
    void offModeStillSetsRequestIdHeader() throws Exception {

        MvcResult result = mockMvc().perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(sink.getEvents()).isEmpty();
        assertThat(result.getResponse().getHeader("X-Request-Id")).isNotBlank();
    }

    @Test
    void fullModeIncludesBodiesAndMasksSensitiveFields() throws Exception {

        MvcResult result = mockMvc().perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer my-token")
                        .content("{\"productId\":\"p_123\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("ord_456");

        HttpExchangeLogEvent event = sink.last();
        assertThat(event.getConfiguredMode()).isEqualTo(HttpLogMode.FULL);
        // Body is kept as a JSON node so it serializes as a nested object, not an escaped string.
        assertThat(event.getRequestBody()).isInstanceOf(JsonNode.class);
        assertThat(event.getRequestBody().toString()).contains("\"password\":\"***\"");
        assertThat(event.getRequestBody().toString()).contains("\"productId\":\"p_123\"");
        assertThat(event.getRequestHeaders()).isNotNull();
        Map<String, List<String>> requestHeaders = event.getRequestHeaders();
        assertThat(extractByKey(requestHeaders, "Authorization")).contains("***");
        assertThat(event.getResponseBody().toString()).contains("ord_456");
    }

    @Test
    void doesNotLeakSecretsWhenBodyIsTruncated() throws Exception {
        // A large JSON body whose secret sits past max-body-length must still be masked,
        // never echoed raw because truncation produced invalid JSON.
        String body = "{\"password\":\"hunter2\",\"filler\":\"%s\"}".formatted("x".repeat(500));

        mockMvc().perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        HttpExchangeLogEvent event = sink.last();
        assertThat(event.getRequestBody().toString()).doesNotContain("hunter2");
    }

    @Test
    void truncatesBodyOverMaxLength() throws Exception {

        String big = "{\"data\":\"%s\"}".formatted("x".repeat(500));

        mockMvc().perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(big))
                .andExpect(status().isOk());

        HttpExchangeLogEvent event = sink.last();
        assertThat(event.isRequestBodyTruncated()).isTrue();
    }

    @Test
    void includesQueryStringForLimitedAndFullModes() throws Exception {

        mockMvc().perform(get("/api/orders/123?source=mobile"))
                .andExpect(status().isOk());

        HttpExchangeLogEvent event = sink.last();
        assertThat(event.getPath()).isEqualTo("/api/orders/123");
        assertThat(event.getQueryString()).isEqualTo("source=mobile");
    }

    @Test
    void capturesExceptionAndStillLogs() throws Exception {

        try {

            mockMvc().perform(get("/api/boom"));
        } catch (Exception ignored) {

            // exception is propagated by MockMvc / dispatcher
        }

        HttpExchangeLogEvent event = sink.last();
        assertThat(event.getExceptionClass()).isEqualTo(IllegalStateException.class.getName());
        assertThat(event.getExceptionMessage()).isEqualTo("Cannot create order");
        // Status must reflect a server error, not the default 200 still on the response.
        assertThat(event.getStatus()).isGreaterThanOrEqualTo(500);
    }

    @Test
    void skipsBinaryContentTypes() throws Exception {

        mockMvc().perform(post("/api/binary")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(new byte[]{1, 2, 3}))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3}));

        HttpExchangeLogEvent event = sink.last();
        assertThat(event.getRequestBody()).asString().contains("not logged");
        assertThat(event.getResponseBody()).asString().contains("not logged");
    }

    @Test
    void responseBodyStillReachesClient() throws Exception {

        MvcResult result = mockMvc().perform(get("/api/orders/123?source=mobile"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("\"orderId\":\"123\"");
        assertThat(result.getResponse().getHeader("X-Request-Id")).isNotBlank();
    }

    @Test
    void honorsIncomingRequestIdHeader() throws Exception {

        MvcResult result = mockMvc().perform(get("/api/orders/123").header("X-Request-Id", "external-id"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getHeader("X-Request-Id")).isEqualTo("external-id");
        HttpExchangeLogEvent event = sink.last();
        assertThat(event.getRequestId()).isEqualTo("external-id");
    }

    private List<String> extractByKey(Map<String, List<String>> headers, String key) {

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {

            if (entry.getKey().equalsIgnoreCase(key)) {

                return entry.getValue();
            }
        }
        return List.of();
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
