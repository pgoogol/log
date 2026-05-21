package com.yourcompany.httpexchangelogger.sanitizer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveValueMaskerTest {

    @Test
    void detectsCaseInsensitiveMatches() {
        SensitiveValueMasker masker = new SensitiveValueMasker(List.of("password", "Token"));

        assertThat(masker.isSensitive("password")).isTrue();
        assertThat(masker.isSensitive("PASSWORD")).isTrue();
        assertThat(masker.isSensitive("Token")).isTrue();
        assertThat(masker.isSensitive("token")).isTrue();
        assertThat(masker.isSensitive("productId")).isFalse();
    }

    @Test
    void returnsMaskForNonNullValues() {
        SensitiveValueMasker masker = new SensitiveValueMasker(List.of("password"));

        assertThat(masker.mask("secret")).isEqualTo("***");
        assertThat(masker.mask(null)).isNull();
    }

    @Test
    void handlesNullFieldsList() {
        SensitiveValueMasker masker = new SensitiveValueMasker(null);

        assertThat(masker.isSensitive("password")).isFalse();
    }
}
