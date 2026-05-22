package com.pgoogol.httpexchangelogger.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;

public interface RequestIdProvider {

    @NonNull
    String getOrCreateRequestId(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response);
}
