package com.pgoogol.httpexchangelogger.support;

import jakarta.servlet.http.HttpServletRequest;

public class DefaultClientIpExtractor implements ClientIpExtractor {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_REAL_IP = "X-Real-IP";

    @Override
    public String extract(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String forwarded = request.getHeader(X_FORWARDED_FOR);
        if (forwarded != null && !forwarded.isBlank()) {
            for (String segment : forwarded.split(",")) {
                String candidate = segment.trim();
                if (!candidate.isEmpty() && !candidate.equalsIgnoreCase("unknown")) {
                    return candidate;
                }
            }
        }

        String realIp = request.getHeader(X_REAL_IP);
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }
}
