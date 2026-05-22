package com.pgoogol.httpexchangelogger.factory;

import com.pgoogol.httpexchangelogger.autoconfigure.HttpExchangeLoggerProperties;
import com.pgoogol.httpexchangelogger.model.HttpExchangeLogEvent;
import com.pgoogol.httpexchangelogger.model.HttpLogMode;
import com.pgoogol.httpexchangelogger.sanitizer.BodySanitizer;
import com.pgoogol.httpexchangelogger.sanitizer.HeaderSanitizer;
import com.pgoogol.httpexchangelogger.support.BodyExtractor;
import com.pgoogol.httpexchangelogger.support.BodyTruncator;
import com.pgoogol.httpexchangelogger.support.ClientIpExtractor;
import com.pgoogol.httpexchangelogger.support.ContentTypeMatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HttpExchangeLogEventFactory {

    private static final int SERVER_ERROR_STATUS = 500;

    private final HttpExchangeLoggerProperties properties;
    private final HeaderSanitizer headerSanitizer;
    private final BodySanitizer bodySanitizer;
    private final ClientIpExtractor clientIpExtractor;
    private final ObjectMapper objectMapper;

    public HttpExchangeLogEventFactory(HttpExchangeLoggerProperties properties,
                                       HeaderSanitizer headerSanitizer,
                                       BodySanitizer bodySanitizer,
                                       ClientIpExtractor clientIpExtractor,
                                       ObjectMapper objectMapper) {
        this.properties = properties;
        this.headerSanitizer = headerSanitizer;
        this.bodySanitizer = bodySanitizer;
        this.clientIpExtractor = clientIpExtractor;
        this.objectMapper = objectMapper;
    }

    public HttpExchangeLogEvent create(ContentCachingRequestWrapper request,
                                       ContentCachingResponseWrapper response,
                                       HttpLogMode mode,
                                       String requestId,
                                       long durationMs,
                                       Throwable exception) {

        HttpExchangeLogEvent.Builder builder = HttpExchangeLogEvent.builder()
                .requestId(requestId)
                .method(request.getMethod())
                .path(request.getRequestURI())
                .status(resolveStatus(response, exception))
                .durationMs(durationMs)
                .configuredMode(mode)
                .effectiveMode(mode);

        if (properties.getInclude().isQueryString()) {
            builder.queryString(request.getQueryString());
        }

        if (exception != null) {
            Throwable root = unwrap(exception);
            builder.exceptionClass(root.getClass().getName());
            builder.exceptionMessage(root.getMessage());
        }

        if (mode == HttpLogMode.BASIC || mode == HttpLogMode.OFF) {
            return builder.build();
        }

        if (properties.getInclude().isClientIp() && clientIpExtractor != null) {
            builder.clientIp(clientIpExtractor.extract(request));
        }

        if (properties.getInclude().isHeaders()) {
            builder.requestHeaders(sanitizeHeaders(extractRequestHeaders(request)));
            builder.responseHeaders(sanitizeHeaders(extractResponseHeaders(response)));
        }

        applyRequestBody(request, builder);
        applyResponseBody(response, builder);

        return builder.build();
    }

    private int resolveStatus(ContentCachingResponseWrapper response, Throwable exception) {
        int status = response.getStatus();
        // When the handler threw, the container has not run its error handling yet,
        // so the wrapper still reports the default 200. Reflect a server error instead.
        if (exception != null && status < 400) {
            return SERVER_ERROR_STATUS;
        }
        return status;
    }

    private void applyRequestBody(ContentCachingRequestWrapper request, HttpExchangeLogEvent.Builder builder) {
        String contentType = request.getContentType();
        BodyResult result = buildBody(BodyExtractor.extractRequestBody(request), contentType);
        if (result == null) {
            return;
        }
        builder.requestBody(result.value());
        builder.requestBodyTruncated(result.truncated());
    }

    private void applyResponseBody(ContentCachingResponseWrapper response, HttpExchangeLogEvent.Builder builder) {
        String contentType = response.getContentType();
        BodyResult result = buildBody(BodyExtractor.extractResponseBody(response), contentType);
        if (result == null) {
            return;
        }
        builder.responseBody(result.value());
        builder.responseBodyTruncated(result.truncated());
    }

    private BodyResult buildBody(String body, String contentType) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        if (properties.getMaxBodyLength() <= 0) {
            // Body logging disabled.
            return null;
        }
        if (contentType != null && ContentTypeMatcher.isBinary(contentType)) {
            return new BodyResult(notLogged(contentType), false);
        }
        if (contentType != null && !ContentTypeMatcher.isLoggable(contentType)) {
            return new BodyResult(notLogged(contentType), false);
        }

        // Mask FIRST on the full body; truncation afterwards can never expose a secret
        // because sensitive values are already replaced.
        String sanitized = bodySanitizer.sanitize(body, contentType);

        BodyTruncator.TruncationResult truncated =
                BodyTruncator.truncate(sanitized, properties.getMaxBodyLength());
        if (truncated.isTruncated()) {
            return new BodyResult(truncated.getValue(), true);
        }

        return new BodyResult(asJsonOrString(truncated.getValue(), contentType), false);
    }

    private Object asJsonOrString(String value, String contentType) {
        if (objectMapper != null && ContentTypeMatcher.isJson(contentType)) {
            try {
                return objectMapper.readTree(value);
            } catch (Exception ex) {
                return value;
            }
        }
        return value;
    }

    private static String notLogged(String contentType) {
        return "[not logged: unsupported content type " + contentType + "]";
    }

    private Map<String, List<String>> extractRequestHeaders(HttpServletRequest request) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        java.util.Enumeration<String> names = request.getHeaderNames();
        if (names == null) {
            return Collections.emptyMap();
        }
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            java.util.Enumeration<String> values = request.getHeaders(name);
            List<String> list = new ArrayList<>();
            if (values != null) {
                while (values.hasMoreElements()) {
                    list.add(values.nextElement());
                }
            }
            headers.put(name, list);
        }
        return headers;
    }

    private Map<String, List<String>> extractResponseHeaders(HttpServletResponse response) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        Collection<String> names = response.getHeaderNames();
        if (names == null) {
            return Collections.emptyMap();
        }
        for (String name : names) {
            Collection<String> values = response.getHeaders(name);
            headers.put(name, values == null ? Collections.emptyList() : new ArrayList<>(values));
        }
        return headers;
    }

    private Map<String, List<String>> sanitizeHeaders(Map<String, List<String>> headers) {
        if (headerSanitizer == null) {
            return headers;
        }
        return headerSanitizer.sanitize(headers);
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof ServletException servletException && servletException.getCause() != null
                && servletException.getCause() != current) {
            current = servletException.getCause();
        }
        return current;
    }

    private record BodyResult(Object value, boolean truncated) {
    }
}
