package com.pgoogol.httpexchangelogger.resolver;

import com.pgoogol.httpexchangelogger.autoconfigure.HttpExchangeLoggerProperties;
import com.pgoogol.httpexchangelogger.model.HttpLogMode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

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

    public HttpLogMode resolve(HttpServletRequest request) {
        return resolve(request.getRequestURI());
    }

    public HttpLogMode resolve(String path) {
        if (path == null) {
            return properties.getDefaultMode();
        }

        for (HttpExchangeLoggerProperties.Endpoint rule : properties.getEndpoints()) {
            if (rule.getPattern() == null || rule.getMode() == null) {
                continue;
            }
            if (pathMatcher.match(rule.getPattern(), path)) {
                return rule.getMode();
            }
        }

        return properties.getDefaultMode();
    }
}
