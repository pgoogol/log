package com.pgoogol.httpexchangelogger.support;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface ClientIpExtractor {

    @Nullable
    String extract(@NonNull HttpServletRequest request);
}
