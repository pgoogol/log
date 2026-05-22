package com.pgoogol.httpexchangelogger.sanitizer;

import org.jspecify.annotations.Nullable;

public interface BodySanitizer {

    @Nullable
    String sanitize(@Nullable String body, @Nullable String contentType);
}
