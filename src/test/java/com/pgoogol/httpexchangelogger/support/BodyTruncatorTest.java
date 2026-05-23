package com.pgoogol.httpexchangelogger.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BodyTruncatorTest {

    @Test
    void doesNotTruncateBelowLimit() {

        BodyTruncator.TruncationResult result = BodyTruncator.truncate("hello", 10);

        assertThat(result.value()).isEqualTo("hello");
        assertThat(result.truncated()).isFalse();
    }

    @Test
    void truncatesAtLimit() {

        BodyTruncator.TruncationResult result = BodyTruncator.truncate("hello world", 5);

        assertThat(result.value()).isEqualTo("hello");
        assertThat(result.truncated()).isTrue();
    }

    @Test
    void handlesNullBody() {

        BodyTruncator.TruncationResult result = BodyTruncator.truncate(null, 5);

        assertThat(result.value()).isNull();
        assertThat(result.truncated()).isFalse();
    }

    @Test
    void handlesZeroLimit() {

        BodyTruncator.TruncationResult result = BodyTruncator.truncate("abc", 0);

        assertThat(result.value()).isEmpty();
        assertThat(result.truncated()).isTrue();
    }

}
