package com.pgoogol.httpexchangelogger.support;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class BodyTruncator {

    private BodyTruncator() {
    }

    public static TruncationResult truncate(@Nullable String body, int maxLength) {
        if (Objects.isNull(body)) {
            return new TruncationResult(null, false);
        }
        if (maxLength <= 0) {
            return new TruncationResult("", true);
        }
        if (body.length() <= maxLength) {
            return new TruncationResult(body, false);
        }
        return new TruncationResult(body.substring(0, maxLength), true);
    }

    public static final class TruncationResult {
        private final @Nullable String value;
        private final boolean truncated;

        public TruncationResult(@Nullable String value, boolean truncated) {
            this.value = value;
            this.truncated = truncated;
        }

        public @Nullable String getValue() {
            return value;
        }

        public boolean isTruncated() {
            return truncated;
        }
    }
}
