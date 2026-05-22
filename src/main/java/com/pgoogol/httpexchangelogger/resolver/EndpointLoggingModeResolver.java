package com.pgoogol.httpexchangelogger.resolver;

import com.pgoogol.httpexchangelogger.autoconfigure.HttpExchangeLoggerProperties;
import com.pgoogol.httpexchangelogger.model.HttpLogMode;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.Objects;

public class EndpointLoggingModeResolver {

    private final HttpExchangeLoggerProperties properties;
    private final PathMatcher pathMatcher;

    public EndpointLoggingModeResolver(HttpExchangeLoggerProperties properties) {
        this(properties, new AntPathMatcher());
    }

    public EndpointLoggingModeResolver(HttpExchangeLoggerProperties properties, PathMatcher pathMatcher) {
        this.properties = properties;
        this.pathMatcher = pathMatcher;
    }

    public @NonNull HttpLogMode resolve(@NonNull HttpServletRequest request) {
        return resolve(request.getRequestURI());
    }

    public @NonNull HttpLogMode resolve(@Nullable String path) {
        if (Objects.isNull(path)) {
            return properties.getDefaultMode();
        }

        for (var rule : properties.getEndpoints()) {
            if (Objects.isNull(rule.getPattern()) || Objects.isNull(rule.getMode())) {
                continue;
            }
            if (pathMatcher.match(rule.getPattern(), path)) {
                return rule.getMode();
            }
        }

        return properties.getDefaultMode();
    }
}
