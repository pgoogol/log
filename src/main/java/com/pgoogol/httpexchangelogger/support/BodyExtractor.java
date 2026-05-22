package com.pgoogol.httpexchangelogger.support;

import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class BodyExtractor {

    private BodyExtractor() {
    }

    public static String extractRequestBody(ContentCachingRequestWrapper request) {
        if (request == null) {
            return null;
        }
        byte[] content = request.getContentAsByteArray();
        if (content == null || content.length == 0) {
            return null;
        }
        return new String(content, resolveCharset(request.getCharacterEncoding()));
    }

    public static String extractResponseBody(ContentCachingResponseWrapper response) {
        if (response == null) {
            return null;
        }
        byte[] content = response.getContentAsByteArray();
        if (content == null || content.length == 0) {
            return null;
        }
        return new String(content, resolveCharset(response.getCharacterEncoding()));
    }

    private static Charset resolveCharset(String encoding) {
        if (encoding == null || encoding.isBlank()) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(encoding);
        } catch (Exception ex) {
            return StandardCharsets.UTF_8;
        }
    }
}
