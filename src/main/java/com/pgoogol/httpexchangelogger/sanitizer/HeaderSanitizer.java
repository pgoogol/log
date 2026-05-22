package com.pgoogol.httpexchangelogger.sanitizer;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface HeaderSanitizer {

    @Nullable
    Map<String, List<String>> sanitize(@Nullable Map<String, List<String>> headers);
}
