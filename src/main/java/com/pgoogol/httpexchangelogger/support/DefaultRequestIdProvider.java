package com.pgoogol.httpexchangelogger.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

    private static boolean isSafe(String value) {

        return !value.isEmpty() && value.length() <= MAX_LENGTH && SAFE_ID.matcher(value).matches();
    }

    @Override
    public String getOrCreateRequestId(HttpServletRequest request, HttpServletResponse response) {

        String requestId = null;
        if (Objects.nonNull(request)) {

            String existing = request.getHeader(HEADER);
            if (Objects.nonNull(existing)) {

                String trimmed = existing.trim();
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

}
