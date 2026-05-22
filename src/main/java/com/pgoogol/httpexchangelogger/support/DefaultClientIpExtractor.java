package com.pgoogol.httpexchangelogger.support;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class DefaultClientIpExtractor implements ClientIpExtractor {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_REAL_IP = "X-Real-IP";

    @Override
    public @Nullable String extract(@NonNull HttpServletRequest request) {
        if (Objects.isNull(request)) {
            return null;
        }

        var forwarded = request.getHeader(X_FORWARDED_FOR);
        if (Objects.nonNull(forwarded) && !forwarded.isBlank()) {
            for (var segment : forwarded.split(",")) {
                var candidate = segment.trim();
                if (!candidate.isEmpty() && !candidate.equalsIgnoreCase("unknown")) {
                    return candidate;
                }
            }
        }

        var realIp = request.getHeader(X_REAL_IP);
        if (Objects.nonNull(realIp) && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }
}
