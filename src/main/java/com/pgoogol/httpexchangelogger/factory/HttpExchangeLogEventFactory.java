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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class HttpExchangeLogEventFactory {

    private static final int SERVER_ERROR_STATUS = 500;

    private final HttpExchangeLoggerProperties properties;
    private final HeaderSanitizer headerSanitizer;
    private final BodySanitizer bodySanitizer;
    private final ClientIpExtractor clientIpExtractor;
    private final ObjectMapper objectMapper;

    public HttpExchangeLogEventFactory(HttpExchangeLoggerProperties properties, HeaderSanitizer headerSanitizer, BodySanitizer bodySanitizer, ClientIpExtractor clientIpExtractor, ObjectMapper objectMapper) {

        this.properties = properties;
        this.headerSanitizer = headerSanitizer;
        this.bodySanitizer = bodySanitizer;
        this.clientIpExtractor = clientIpExtractor;
        this.objectMapper = objectMapper;
    }

    private static String notLogged(String contentType) {

        return "[not logged: unsupported content type %s]".formatted(contentType);
    }

    private static Throwable unwrap(Throwable throwable) {

        Throwable current = throwable;
        while (current instanceof ServletException servletException && servletException.getCause() != null && servletException.getCause() != current) {
            current = servletException.getCause();
        }
        return current;
    }

    public HttpExchangeLogEvent create(HttpServletRequest request, byte[] requestBody, ContentCachingResponseWrapper response, HttpLogMode mode, String requestId, long durationMs, Throwable exception) {

        HttpExchangeLogEvent.Builder builder = HttpExchangeLogEvent.builder().requestId(requestId).method(request.getMethod()).path(request.getRequestURI()).status(resolveStatus(response, exception)).durationMs(durationMs).configuredMode(mode).effectiveMode(mode);

        HttpExchangeLoggerProperties.Include propertiesInclude = properties.getInclude();
        if (propertiesInclude.isQueryString()) {

            builder.queryString(request.getQueryString());
        }

        if (Objects.nonNull(exception)) {

            Throwable root = unwrap(exception);
            builder.exceptionClass(root.getClass().getName());
            builder.exceptionMessage(root.getMessage());
        }

        if (CollectionUtils.containsInstance(Set.of(HttpLogMode.BASIC, HttpLogMode.OFF), mode)) {

            return builder.build();
        }

        if (propertiesInclude.isClientIp() && Objects.nonNull(clientIpExtractor)) {

            builder.clientIp(clientIpExtractor.extract(request));
        }

        if (propertiesInclude.isHeaders()) {

            builder.requestHeaders(sanitizeHeaders(extractRequestHeaders(request)));
            builder.responseHeaders(sanitizeHeaders(extractResponseHeaders(response)));
        }

        applyRequestBody(request, requestBody, builder);
        applyResponseBody(response, builder);

        return builder.build();
    }

    private int resolveStatus(ContentCachingResponseWrapper response, Throwable exception) {

        int status = response.getStatus();
        // When the handler threw, the container has not run its error handling yet,
        // so the wrapper still reports the default 200. Reflect a server error instead.
        if (Objects.nonNull(exception) && status < 400) {

            return SERVER_ERROR_STATUS;
        }
        return status;
    }

    private void applyRequestBody(HttpServletRequest request, byte[] requestBody, HttpExchangeLogEvent.Builder builder) {

        String contentType = request.getContentType();
        String body = BodyExtractor.toBodyString(requestBody, request.getCharacterEncoding());
        // Only bounded textual request bodies are captured. When the request carried a body of an
        // unloggable type (binary, multipart) it is never read, so emit the same placeholder the
        // response path would instead of silently dropping it.
        if (Objects.isNull(body) && carriesUnloggableBody(request, contentType)) {

            builder.requestBody(notLogged(contentType));
            return;
        }
        BodyResult result = buildBody(body, contentType);
        if (Objects.isNull(result)) {

            return;
        }
        builder.requestBody(result.value());
        builder.requestBodyTruncated(result.truncated());
    }

    private boolean carriesUnloggableBody(HttpServletRequest request, String contentType) {

        if (request.getContentLengthLong() <= 0 || Objects.isNull(contentType)) {

            return false;
        }
        return ContentTypeMatcher.isBinary(contentType) || !ContentTypeMatcher.isLoggable(contentType);
    }

    private void applyResponseBody(ContentCachingResponseWrapper response, HttpExchangeLogEvent.Builder builder) {

        String body = BodyExtractor.toBodyString(response.getContentAsByteArray(), response.getCharacterEncoding());
        BodyResult result = buildBody(body, response.getContentType());
        if (Objects.isNull(result)) {

            return;
        }
        builder.responseBody(result.value());
        builder.responseBodyTruncated(result.truncated());
    }

    private BodyResult buildBody(String body, String contentType) {

        if (!StringUtils.hasLength(body)) {

            return null;
        }
        if (properties.getMaxBodyLength() <= 0) {

            // Body logging disabled.
            return null;
        }
        if (Objects.nonNull(contentType) && ContentTypeMatcher.isBinary(contentType)) {

            return new BodyResult(notLogged(contentType), false);
        }
        if (Objects.nonNull(contentType) && !ContentTypeMatcher.isLoggable(contentType)) {

            return new BodyResult(notLogged(contentType), false);
        }

        // Mask FIRST on the full body; truncation afterwards can never expose a secret
        // because sensitive values are already replaced.
        String sanitized = bodySanitizer.sanitize(body, contentType);

        BodyTruncator.TruncationResult truncated = BodyTruncator.truncate(sanitized, properties.getMaxBodyLength());
        if (truncated.truncated()) {

            return new BodyResult(truncated.value(), true);
        }

        return new BodyResult(asJsonOrString(truncated.value(), contentType), false);
    }

    private Object asJsonOrString(String value, String contentType) {

        if (Objects.nonNull(objectMapper) && ContentTypeMatcher.isJson(contentType)) {

            try {

                return objectMapper.readTree(value);
            } catch (Exception ex) {

                return value;
            }
        }
        return value;
    }

    private Map<String, List<String>> extractRequestHeaders(HttpServletRequest request) {

        Enumeration<String> names = request.getHeaderNames();
        if (Objects.isNull(names)) {

            return Collections.emptyMap();
        }
        Map<String, List<String>> headers = new LinkedHashMap<>();
        Collections.list(names).forEach(name -> headers.put(name, requestHeaderValues(request, name)));
        return headers;
    }

    private List<String> requestHeaderValues(HttpServletRequest request, String name) {

        Enumeration<String> values = request.getHeaders(name);
        if (Objects.isNull(values)) {

            return Collections.emptyList();
        }
        return Collections.list(values);
    }

    private Map<String, List<String>> extractResponseHeaders(HttpServletResponse response) {

        Collection<String> names = response.getHeaderNames();
        if (Objects.isNull(names)) {

            return Collections.emptyMap();
        }
        Map<String, List<String>> headers = new LinkedHashMap<>();
        names.forEach(name -> headers.put(name, responseHeaderValues(response, name)));
        return headers;
    }

    private List<String> responseHeaderValues(HttpServletResponse response, String name) {

        Collection<String> values = response.getHeaders(name);
        if (Objects.isNull(values)) {

            return Collections.emptyList();
        }
        return new ArrayList<>(values);
    }

    private Map<String, List<String>> sanitizeHeaders(Map<String, List<String>> headers) {

        if (Objects.isNull(headerSanitizer)) {

            return headers;
        }
        return headerSanitizer.sanitize(headers);
    }

    private record BodyResult(Object value, boolean truncated) {

    }

}
