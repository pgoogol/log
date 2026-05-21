package com.yourcompany.httpexchangelogger.sanitizer;

import com.yourcompany.httpexchangelogger.autoconfigure.HttpExchangeLoggerProperties;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultHeaderSanitizerTest {

    private DefaultHeaderSanitizer sanitizerWithFields(List<String> fields, boolean enabled) {
        HttpExchangeLoggerProperties.Mask mask = new HttpExchangeLoggerProperties.Mask();
        mask.setEnabled(enabled);
        mask.setFields(fields);
        return new DefaultHeaderSanitizer(new SensitiveValueMasker(fields), mask);
    }

    @Test
    void masksDefaultSensitiveHeadersEvenWhenNotInList() {
        DefaultHeaderSanitizer sanitizer = sanitizerWithFields(List.of(), true);
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Authorization", List.of("Bearer secret"));
        headers.put("Cookie", List.of("session=xyz"));
        headers.put("X-Api-Key", List.of("key1"));
        headers.put("Content-Type", List.of("application/json"));

        Map<String, List<String>> result = sanitizer.sanitize(headers);

        assertThat(result.get("Authorization")).containsExactly("***");
        assertThat(result.get("Cookie")).containsExactly("***");
        assertThat(result.get("X-Api-Key")).containsExactly("***");
        assertThat(result.get("Content-Type")).containsExactly("application/json");
    }

    @Test
    void caseInsensitiveHeaderMatching() {
        DefaultHeaderSanitizer sanitizer = sanitizerWithFields(List.of("x-trace"), true);
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("AUTHORIZATION", List.of("Bearer secret"));
        headers.put("X-Trace", List.of("trace-id"));

        Map<String, List<String>> result = sanitizer.sanitize(headers);

        assertThat(result.get("AUTHORIZATION")).containsExactly("***");
        assertThat(result.get("X-Trace")).containsExactly("***");
    }

    @Test
    void masksAllValuesForMultiValueHeaders() {
        DefaultHeaderSanitizer sanitizer = sanitizerWithFields(List.of(), true);
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Set-Cookie", List.of("a=1", "b=2"));

        Map<String, List<String>> result = sanitizer.sanitize(headers);

        assertThat(result.get("Set-Cookie")).containsExactly("***", "***");
    }

    @Test
    void doesNotMaskWhenDisabled() {
        DefaultHeaderSanitizer sanitizer = sanitizerWithFields(List.of("authorization"), false);
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Authorization", List.of("Bearer secret"));

        Map<String, List<String>> result = sanitizer.sanitize(headers);

        assertThat(result.get("Authorization")).containsExactly("Bearer secret");
    }
}
