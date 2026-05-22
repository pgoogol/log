package com.pgoogol.httpexchangelogger.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BodyTruncatorTest {

    @Test
    void truncate_whenBodyBelowLimit_doesNotTruncate() {
        BodyTruncator.TruncationResult result = BodyTruncator.truncate("hello", 10);

        assertThat(result.getValue()).isEqualTo("hello");
        assertThat(result.isTruncated()).isFalse();
    }

    @Test
    void truncate_whenBodyExceedsLimit_truncatesAtLimit() {
        BodyTruncator.TruncationResult result = BodyTruncator.truncate("hello world", 5);

        assertThat(result.getValue()).isEqualTo("hello");
        assertThat(result.isTruncated()).isTrue();
    }

    @Test
    void truncate_whenBodyIsNull_returnsNullNotTruncated() {
        BodyTruncator.TruncationResult result = BodyTruncator.truncate(null, 5);

        assertThat(result.getValue()).isNull();
        assertThat(result.isTruncated()).isFalse();
    }

    @Test
    void truncate_whenLimitIsZero_returnsEmptyTruncated() {
        BodyTruncator.TruncationResult result = BodyTruncator.truncate("abc", 0);

        assertThat(result.getValue()).isEmpty();
        assertThat(result.isTruncated()).isTrue();
    }
}
