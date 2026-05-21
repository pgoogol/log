package com.yourcompany.httpexchangelogger.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.UUID;

public class DefaultRequestIdProvider implements RequestIdProvider {

    public static final String HEADER = "X-Request-Id";

    @Override
    public String getOrCreateRequestId(HttpServletRequest request, HttpServletResponse response) {
        String requestId = null;
        if (request != null) {
            String existing = request.getHeader(HEADER);
            if (existing != null && !existing.isBlank()) {
                requestId = existing.trim();
            }
        }
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        if (response != null && response.getHeader(HEADER) == null) {
            response.setHeader(HEADER, requestId);
        }
        return requestId;
    }
}
