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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HttpExchangeLogEventFactory {

    private final HttpExchangeLoggerProperties properties;
    private final HeaderSanitizer headerSanitizer;
    private final BodySanitizer bodySanitizer;
    private final ClientIpExtractor clientIpExtractor;

    public HttpExchangeLogEventFactory(HttpExchangeLoggerProperties properties,
                                       HeaderSanitizer headerSanitizer,
                                       BodySanitizer bodySanitizer,
                                       ClientIpExtractor clientIpExtractor) {
        this.properties = properties;
        this.headerSanitizer = headerSanitizer;
        this.bodySanitizer = bodySanitizer;
        this.clientIpExtractor = clientIpExtractor;
    }

    public HttpExchangeLogEvent create(ContentCachingRequestWrapper request,
                                       ContentCachingResponseWrapper response,
                                       HttpLogMode mode,
                                       String requestId,
                                       long durationMs,
                                       Throwable exception) {

        HttpExchangeLogEvent event = new HttpExchangeLogEvent();
        event.setRequestId(requestId);
        event.setMethod(request.getMethod());
        event.setPath(request.getRequestURI());
        event.setStatus(response.getStatus());
        event.setDurationMs(durationMs);
        event.setConfiguredMode(mode);
        event.setEffectiveMode(mode);

        if (properties.getInclude().isQueryString()) {
            event.setQueryString(request.getQueryString());
        }

        if (exception != null) {
            Throwable root = unwrap(exception);
            event.setExceptionClass(root.getClass().getName());
            event.setExceptionMessage(root.getMessage());
        }

        if (mode == HttpLogMode.BASIC || mode == HttpLogMode.OFF) {
            return event;
        }

        if (properties.getInclude().isClientIp() && clientIpExtractor != null) {
            event.setClientIp(clientIpExtractor.extract(request));
        }

        if (properties.getInclude().isHeaders()) {
            event.setRequestHeaders(sanitizeHeaders(extractRequestHeaders(request)));
            event.setResponseHeaders(sanitizeHeaders(extractResponseHeaders(response)));
        }

        applyRequestBody(request, event);
        applyResponseBody(response, event);

        return event;
    }

    private void applyRequestBody(ContentCachingRequestWrapper request, HttpExchangeLogEvent event) {
        String contentType = request.getContentType();
        BodyResult result = buildBody(BodyExtractor.extractRequestBody(request), contentType);
        if (result == null) {
            return;
        }
        event.setRequestBody(result.value());
        event.setRequestBodyTruncated(result.truncated());
    }

    private void applyResponseBody(ContentCachingResponseWrapper response, HttpExchangeLogEvent event) {
        String contentType = response.getContentType();
        BodyResult result = buildBody(BodyExtractor.extractResponseBody(response), contentType);
        if (result == null) {
            return;
        }
        event.setResponseBody(result.value());
        event.setResponseBodyTruncated(result.truncated());
    }

    private BodyResult buildBody(String body, String contentType) {
        if (contentType != null && ContentTypeMatcher.isBinary(contentType)) {
            return new BodyResult("[not logged: unsupported content type " + contentType + "]", false);
        }
        if (body == null || body.isEmpty()) {
            return null;
        }
        if (contentType != null && !ContentTypeMatcher.isLoggable(contentType)) {
            return new BodyResult("[not logged: unsupported content type " + contentType + "]", false);
        }

        BodyTruncator.TruncationResult truncated = BodyTruncator.truncate(body, properties.getMaxBodyLength());
        String sanitized = bodySanitizer.sanitize(truncated.getValue(), contentType);
        return new BodyResult(sanitized, truncated.isTruncated());
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
