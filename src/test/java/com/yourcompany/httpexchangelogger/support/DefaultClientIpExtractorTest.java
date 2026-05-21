package com.yourcompany.httpexchangelogger.support;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultClientIpExtractorTest {

    private final DefaultClientIpExtractor extractor = new DefaultClientIpExtractor();

    @Test
    void prefersFirstAddressFromXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
        request.setRemoteAddr("127.0.0.1");

        assertThat(extractor.extract(request)).isEqualTo("10.0.0.1");
    }

    @Test
    void fallsBackToXRealIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "192.168.0.1");
        request.setRemoteAddr("127.0.0.1");

        assertThat(extractor.extract(request)).isEqualTo("192.168.0.1");
    }

    @Test
    void fallsBackToRemoteAddrWhenNoHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        assertThat(extractor.extract(request)).isEqualTo("127.0.0.1");
    }
}
