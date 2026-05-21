package com.yourcompany.httpexchangelogger.support;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRequestIdProviderTest {

    private final DefaultRequestIdProvider provider = new DefaultRequestIdProvider();

    @Test
    void usesIncomingHeaderWhenPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "external-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String result = provider.getOrCreateRequestId(request, response);

        assertThat(result).isEqualTo("external-id");
        assertThat(response.getHeader("X-Request-Id")).isEqualTo("external-id");
    }

    @Test
    void generatesUuidWhenHeaderMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        String result = provider.getOrCreateRequestId(request, response);

        assertThat(result).isNotBlank();
        UUID.fromString(result);
        assertThat(response.getHeader("X-Request-Id")).isEqualTo(result);
    }

    @Test
    void preservesAlreadySetResponseHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setHeader("X-Request-Id", "preset");

        String result = provider.getOrCreateRequestId(request, response);

        assertThat(result).isNotBlank();
        assertThat(response.getHeader("X-Request-Id")).isEqualTo("preset");
    }
}
