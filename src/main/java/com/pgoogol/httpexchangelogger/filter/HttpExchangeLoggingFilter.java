package com.pgoogol.httpexchangelogger.filter;

import com.pgoogol.httpexchangelogger.autoconfigure.HttpExchangeLoggerProperties;
import com.pgoogol.httpexchangelogger.factory.HttpExchangeLogEventFactory;
import com.pgoogol.httpexchangelogger.model.HttpExchangeLogEvent;
import com.pgoogol.httpexchangelogger.model.HttpLogMode;
import com.pgoogol.httpexchangelogger.resolver.EndpointLoggingModeResolver;
import com.pgoogol.httpexchangelogger.sink.HttpExchangeLogSink;
import com.pgoogol.httpexchangelogger.support.CachedBodyHttpServletRequest;
import com.pgoogol.httpexchangelogger.support.ContentTypeMatcher;
import com.pgoogol.httpexchangelogger.support.RequestIdProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
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

        HttpLogMode mode = modeResolver.resolve(request);

        // Correlation id is established for every request the filter sees (including OFF),
        // so downstream consumers always get an X-Request-Id.
        String requestId = requestIdProvider.getOrCreateRequestId(request, response);

        if (mode == HttpLogMode.OFF) {

            filterChain.doFilter(request, response);
            return;
        }

        int cacheLimit = resolveCacheLimit(properties.getMaxBodyLength());
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        // Eagerly buffer bounded textual bodies so they are logged even when the handler never
        // reads them; everything else falls back to read-through caching (form params, multipart,
        // large or unknown-length bodies) to avoid breaking the application or buffering uploads.
        CachedBodyHttpServletRequest cachedRequest = null;
        ContentCachingRequestWrapper contentCachingRequest = null;
        HttpServletRequest requestToUse;
        if (shouldBufferEagerly(request, cacheLimit)) {

            cachedRequest = new CachedBodyHttpServletRequest(request);
            requestToUse = cachedRequest;
        } else {

            contentCachingRequest = new ContentCachingRequestWrapper(request, cacheLimit);
            requestToUse = contentCachingRequest;
        }

        long startedAt = System.nanoTime();
        Throwable exception = null;

        try {

            filterChain.doFilter(requestToUse, wrappedResponse);
        } catch (ServletException | IOException | RuntimeException ex) {

            exception = ex;
            throw ex;
        } finally {

            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            try {

                byte[] requestBody = Objects.nonNull(cachedRequest)
                        ? cachedRequest.getCachedBody()
                        : contentCachingRequest.getContentAsByteArray();
                HttpExchangeLogEvent event = eventFactory.create(
                        requestToUse,
                        requestBody,
                        wrappedResponse,
                        mode,
                        requestId,
                        durationMs,
                        exception
                );
                sink.log(event);
            } catch (RuntimeException loggingException) {

                LOG.warn("Failed to build or emit HTTP exchange log event", loggingException);
            } finally {

                copyBodyToResponse(wrappedResponse, exception);
            }
        }
    }

    private boolean shouldBufferEagerly(HttpServletRequest request, int cacheLimit) {

        if (properties.getMaxBodyLength() <= 0) {

            return false;
        }
        String contentType = request.getContentType();
        boolean textual = ContentTypeMatcher.isJson(contentType)
                || ContentTypeMatcher.isXml(contentType)
                || ContentTypeMatcher.isText(contentType);
        if (!textual) {

            return false;
        }
        long length = request.getContentLengthLong();
        return length > 0 && length <= cacheLimit;
    }

    private void copyBodyToResponse(ContentCachingResponseWrapper wrappedResponse, Throwable inFlight)
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
