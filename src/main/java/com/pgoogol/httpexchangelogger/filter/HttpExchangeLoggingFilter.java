package com.pgoogol.httpexchangelogger.filter;

import com.pgoogol.httpexchangelogger.autoconfigure.HttpExchangeLoggerProperties;
import com.pgoogol.httpexchangelogger.factory.HttpExchangeLogEventFactory;
import com.pgoogol.httpexchangelogger.model.HttpExchangeLogEvent;
import com.pgoogol.httpexchangelogger.model.HttpLogMode;
import com.pgoogol.httpexchangelogger.resolver.EndpointLoggingModeResolver;
import com.pgoogol.httpexchangelogger.sink.HttpExchangeLogSink;
import com.pgoogol.httpexchangelogger.support.RequestIdProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Objects;

public class HttpExchangeLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(HttpExchangeLoggingFilter.class);

    private final HttpExchangeLoggerProperties properties;
    private final EndpointLoggingModeResolver modeResolver;
    private final HttpExchangeLogEventFactory eventFactory;
    private final HttpExchangeLogSink sink;
    private final RequestIdProvider requestIdProvider;

    public HttpExchangeLoggingFilter(HttpExchangeLoggerProperties properties,
                                     EndpointLoggingModeResolver modeResolver,
                                     HttpExchangeLogEventFactory eventFactory,
                                     HttpExchangeLogSink sink,
                                     RequestIdProvider requestIdProvider) {
        this.properties = properties;
        this.modeResolver = modeResolver;
        this.eventFactory = eventFactory;
        this.sink = sink;
        this.requestIdProvider = requestIdProvider;
    }

    private static int resolveCacheLimit(int maxBodyLength) {
        if (maxBodyLength <= 0) {
            return 1;
        }
        long expanded = (long) maxBodyLength * 4L + 1L;
        if (expanded > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) expanded;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !properties.isEnabled();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        var mode = modeResolver.resolve(request);

        // Correlation id is established for every request the filter sees (including OFF),
        // so downstream consumers always get an X-Request-Id.
        var requestId = requestIdProvider.getOrCreateRequestId(request, response);

        if (mode == HttpLogMode.OFF) {
            filterChain.doFilter(request, response);
            return;
        }

        var wrappedRequest =
                new ContentCachingRequestWrapper(request, resolveCacheLimit(properties.getMaxBodyLength()));
        var wrappedResponse = new ContentCachingResponseWrapper(response);

        var startedAt = System.nanoTime();
        Throwable exception = null;

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } catch (ServletException | IOException | RuntimeException ex) {
            exception = ex;
            throw ex;
        } finally {
            var durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            try {
                var event = eventFactory.create(wrappedRequest, wrappedResponse, mode, requestId, durationMs, exception);
                sink.log(event);
            } catch (RuntimeException loggingException) {
                LOG.warn("Failed to build or emit HTTP exchange log event", loggingException);
            } finally {
                copyBodyToResponse(wrappedResponse, exception);
            }
        }
    }

    private void copyBodyToResponse(ContentCachingResponseWrapper wrappedResponse, @Nullable Throwable inFlight)
            throws IOException {
        try {
            wrappedResponse.copyBodyToResponse();
        } catch (IOException copyException) {
            // Never let a copy failure mask the exception that is already propagating.
            if (Objects.nonNull(inFlight)) {
                inFlight.addSuppressed(copyException);
            } else {
                throw copyException;
            }
        }
    }
}
