package com.pgoogol.httpexchangelogger.support;

public final class BodyTruncator {

    private BodyTruncator() {
    }

    public static TruncationResult truncate(String body, int maxLength) {
        if (body == null) {
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
        private final String value;
        private final boolean truncated;

        public TruncationResult(String value, boolean truncated) {
            this.value = value;
            this.truncated = truncated;
        }

        public String getValue() {
            return value;
        }

        public boolean isTruncated() {
            return truncated;
        }
    }
}
