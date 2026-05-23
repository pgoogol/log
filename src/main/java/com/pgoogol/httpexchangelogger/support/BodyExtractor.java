package com.pgoogol.httpexchangelogger.support;

import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class BodyExtractor {

    private BodyExtractor() {

    }

    public static String extractRequestBody(ContentCachingRequestWrapper request) {

        if (Objects.isNull(request)) {

            return null;
        }
        byte[] content = request.getContentAsByteArray();
        if (Objects.isNull(content) || content.length == 0) {

            return null;
        }
        return new String(content, resolveCharset(request.getCharacterEncoding()));
    }

    public static String extractResponseBody(ContentCachingResponseWrapper response) {

        if (Objects.isNull(response)) {

            return null;
        }
        byte[] content = response.getContentAsByteArray();
        if (Objects.isNull(content) || content.length == 0) {

            return null;
        }
        return new String(content, resolveCharset(response.getCharacterEncoding()));
    }

    private static Charset resolveCharset(String encoding) {

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
