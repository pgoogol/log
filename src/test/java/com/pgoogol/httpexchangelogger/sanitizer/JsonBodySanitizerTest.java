package com.pgoogol.httpexchangelogger.sanitizer;

import com.pgoogol.httpexchangelogger.autoconfigure.HttpExchangeLoggerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonBodySanitizerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    private JsonBodySanitizer sanitizerWithFields(List<String> fields) {
        HttpExchangeLoggerProperties.Mask mask = new HttpExchangeLoggerProperties.Mask();
        mask.setEnabled(true);
        mask.setFields(fields);
        SensitiveValueMasker masker = new SensitiveValueMasker(fields);
        return new JsonBodySanitizer(objectMapper, masker, mask);
    }

    private JsonBodySanitizer disabledSanitizer() {
        HttpExchangeLoggerProperties.Mask mask = new HttpExchangeLoggerProperties.Mask();
        mask.setEnabled(false);
        mask.setFields(List.of("password"));
        SensitiveValueMasker masker = new SensitiveValueMasker(mask.getFields());
        return new JsonBodySanitizer(objectMapper, masker, mask);
    }

    @Test
    void sanitize_whenTopLevelSensitiveFields_masksThem() {
        JsonBodySanitizer sanitizer = sanitizerWithFields(List.of("password", "email"));
        String input = "{\"email\":\"u@x.com\",\"password\":\"secret\",\"productId\":\"p_123\"}";

        String result = sanitizer.sanitize(input, "application/json");

        assertThat(result).contains("\"email\":\"***\"");
        assertThat(result).contains("\"password\":\"***\"");
        assertThat(result).contains("\"productId\":\"p_123\"");
    }

    @Test
    void sanitize_whenNestedAndArrayFields_masksThem() {
        JsonBodySanitizer sanitizer = sanitizerWithFields(List.of("password", "token"));
        String input = "{\"user\":{\"password\":\"x\"},\"items\":[{\"token\":\"abc\"}]}";

        String result = sanitizer.sanitize(input, "application/json");

        assertThat(result).contains("\"password\":\"***\"");
        assertThat(result).contains("\"token\":\"***\"");
    }

    @Test
    void sanitize_whenFieldNamesDifferInCase_masksCaseInsensitively() {
        JsonBodySanitizer sanitizer = sanitizerWithFields(List.of("token"));
        String input = "{\"Token\":\"a\",\"TOKEN\":\"b\",\"accessToken\":\"c\"}";

        String result = sanitizer.sanitize(input, "application/json");

        assertThat(result).contains("\"Token\":\"***\"");
        assertThat(result).contains("\"TOKEN\":\"***\"");
        assertThat(result).contains("\"accessToken\":\"c\"");
    }

    @Test
    void sanitize_whenMaskingDisabled_returnsOriginalBody() {
        JsonBodySanitizer sanitizer = disabledSanitizer();
        String input = "{\"password\":\"secret\"}";

        String result = sanitizer.sanitize(input, "application/json");

        assertThat(result).isEqualTo(input);
    }

    @Test
    void sanitize_whenContentTypeIsNotJson_returnsOriginalBody() {
        JsonBodySanitizer sanitizer = sanitizerWithFields(List.of("password"));
        String input = "password=secret";

        String result = sanitizer.sanitize(input, "application/x-www-form-urlencoded");

        assertThat(result).isEqualTo(input);
    }

    @Test
    void sanitize_whenInvalidJsonAndSensitiveFieldsConfigured_failsClosedWithPlaceholder() {
        JsonBodySanitizer sanitizer = sanitizerWithFields(List.of("password"));
        String invalid = "{not-json";

        String result = sanitizer.sanitize(invalid, "application/json");

        // Never echo a body we could not parse/mask when masking sensitive fields is requested.
        assertThat(result).isEqualTo(JsonBodySanitizer.UNPARSEABLE_PLACEHOLDER);
        assertThat(result).doesNotContain("not-json");
    }

    @Test
    void sanitize_whenInvalidJsonAndNoSensitiveFields_returnsOriginalBody() {
        JsonBodySanitizer sanitizer = sanitizerWithFields(List.of());
        String invalid = "{not-json";

        String result = sanitizer.sanitize(invalid, "application/json");

        assertThat(result).isEqualTo(invalid);
    }

    @Test
    void sanitize_whenVendorJsonContentType_masksFields() {
        JsonBodySanitizer sanitizer = sanitizerWithFields(List.of("password"));
        String input = "{\"password\":\"secret\"}";

        String result = sanitizer.sanitize(input, "application/hal+json;charset=utf-8");

        assertThat(result).contains("\"password\":\"***\"");
    }

    @Test
    void sanitize_whenBodyIsNull_returnsNull() {
        JsonBodySanitizer sanitizer = sanitizerWithFields(List.of("password"));

        assertThat(sanitizer.sanitize(null, "application/json")).isNull();
    }
}
