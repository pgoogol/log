package com.pgoogol.httpexchangelogger.integration;

import com.pgoogol.httpexchangelogger.filter.HttpExchangeLoggingFilter;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = HttpExchangeLoggingFilterSamplingTest.TestApp.class)
@TestPropertySource(properties = {
        "http-exchange-logger.default-mode=FULL",
        "http-exchange-logger.sampling.rate=0.0"
})
class HttpExchangeLoggingFilterSamplingTest {

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

    @Test
    void doFilter_whenSamplingRateIsZero_emitsNoEventsButStillHandlesRequest() throws Exception {

        // given / when
        MvcResult result = mockMvc().perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"p_1\"}"))
                .andExpect(status().isOk())
                .andReturn();

        // then the request went through untouched, the request id is still issued, but nothing is logged
        assertThat(sink.getEvents()).isEmpty();
        assertThat(result.getResponse().getHeader("X-Request-Id")).isNotBlank();
        assertThat(result.getResponse().getContentAsString()).contains("ord_456");
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
