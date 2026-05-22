package com.pgoogol.httpexchangelogger.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public class DefaultRequestIdProvider implements RequestIdProvider {

    public static final String HEADER = "X-Request-Id";

    private static final int MAX_LENGTH = 200;

    // Conservative whitelist: visible ASCII excluding characters that enable header
    // injection / response splitting (CR, LF, control chars). Untrusted incoming
    // ids that don't match are discarded in favour of a generated UUID.
    private static final Pattern SAFE_ID = Pattern.compile("[\\x21-\\x7E]{1,200}");

    @Override
    public @NonNull String getOrCreateRequestId(@NonNull HttpServletRequest request,
                                                @NonNull HttpServletResponse response) {
        String requestId = null;
        if (Objects.nonNull(request)) {
            var existing = request.getHeader(HEADER);
            if (Objects.nonNull(existing)) {
                var trimmed = existing.trim();
                if (isSafe(trimmed)) {
                    requestId = trimmed;
                }
            }
        }
        if (Objects.isNull(requestId)) {
            requestId = UUID.randomUUID().toString();
        }
        if (Objects.nonNull(response) && Objects.isNull(response.getHeader(HEADER))) {
            response.setHeader(HEADER, requestId);
        }
        return requestId;
    }

    private static boolean isSafe(String value) {
        return !value.isEmpty() && value.length() <= MAX_LENGTH && SAFE_ID.matcher(value).matches();
    }
}
