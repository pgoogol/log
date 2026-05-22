package com.pgoogol.httpexchangelogger.support;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRequestIdProviderTest {

    private final DefaultRequestIdProvider provider = new DefaultRequestIdProvider();

    @Test
    void getOrCreateRequestId_whenIncomingHeaderPresent_usesIt() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "external-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String result = provider.getOrCreateRequestId(request, response);

        assertThat(result).isEqualTo("external-id");
        assertThat(response.getHeader("X-Request-Id")).isEqualTo("external-id");
    }

    @Test
    void getOrCreateRequestId_whenHeaderMissing_generatesUuid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        String result = provider.getOrCreateRequestId(request, response);

        assertThat(result).isNotBlank();
        UUID.fromString(result);
        assertThat(response.getHeader("X-Request-Id")).isEqualTo(result);
    }

    @Test
    void getOrCreateRequestId_whenResponseHeaderAlreadySet_preservesIt() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setHeader("X-Request-Id", "preset");

        String result = provider.getOrCreateRequestId(request, response);

        assertThat(result).isNotBlank();
        assertThat(response.getHeader("X-Request-Id")).isEqualTo("preset");
    }

    @Test
    void getOrCreateRequestId_whenHeaderHasCrlfInjection_rejectsItAndGeneratesUuid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "abc\r\nSet-Cookie: evil=1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String result = provider.getOrCreateRequestId(request, response);

        assertThat(result).doesNotContain("\r").doesNotContain("\n").doesNotContain("Set-Cookie");
        UUID.fromString(result);
        assertThat(response.getHeader("X-Request-Id")).isEqualTo(result);
    }

    @Test
    void getOrCreateRequestId_whenHeaderIsOverlong_rejectsItAndGeneratesUuid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "a".repeat(5000));
        MockHttpServletResponse response = new MockHttpServletResponse();

        String result = provider.getOrCreateRequestId(request, response);

        UUID.fromString(result);
    }
}
