package com.pgoogol.httpexchangelogger.support;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class BodyExtractor {

    private BodyExtractor() {
    }

    public static @Nullable String extractRequestBody(@NonNull ContentCachingRequestWrapper request) {
        if (Objects.isNull(request)) {
            return null;
        }
        var content = request.getContentAsByteArray();
        if (Objects.isNull(content) || content.length == 0) {
            return null;
        }
        return new String(content, resolveCharset(request.getCharacterEncoding()));
    }

    public static @Nullable String extractResponseBody(@NonNull ContentCachingResponseWrapper response) {
        if (Objects.isNull(response)) {
            return null;
        }
        var content = response.getContentAsByteArray();
        if (Objects.isNull(content) || content.length == 0) {
            return null;
        }
        return new String(content, resolveCharset(response.getCharacterEncoding()));
    }

    private static Charset resolveCharset(@Nullable String encoding) {
        if (Objects.isNull(encoding) || encoding.isBlank()) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(encoding);
        } catch (Exception ex) {
            return StandardCharsets.UTF_8;
        }
    }
}
