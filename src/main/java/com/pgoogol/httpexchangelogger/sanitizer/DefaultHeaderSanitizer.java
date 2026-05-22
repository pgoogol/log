package com.pgoogol.httpexchangelogger.sanitizer;

import com.pgoogol.httpexchangelogger.autoconfigure.HttpExchangeLoggerProperties;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class DefaultHeaderSanitizer implements HeaderSanitizer {

    private static final List<String> DEFAULT_SENSITIVE_HEADERS = List.of(
            "authorization",
            "cookie",
            "set-cookie",
            "x-api-key"
    );

    private final SensitiveValueMasker masker;
    private final SensitiveValueMasker defaultHeaderMasker;
    private final boolean maskingEnabled;

    public DefaultHeaderSanitizer(SensitiveValueMasker masker,
                                  HttpExchangeLoggerProperties.@Nullable Mask maskProperties) {
        this.masker = masker;
        this.defaultHeaderMasker = new SensitiveValueMasker(DEFAULT_SENSITIVE_HEADERS);
        this.maskingEnabled = Objects.nonNull(maskProperties) && maskProperties.isEnabled();
    }

    @Override
    public @Nullable Map<String, List<String>> sanitize(@Nullable Map<String, List<String>> headers) {
        if (Objects.isNull(headers) || headers.isEmpty()) {
            return headers;
        }

        var result = new LinkedHashMap<String, List<String>>();
        for (var entry : headers.entrySet()) {
            var name = entry.getKey();
            var values = entry.getValue();

            if (isSensitiveHeader(name)) {
                result.put(name, maskAll(values));
            } else {
                result.put(name, new ArrayList<>(Objects.requireNonNullElse(values, List.of())));
            }
        }
        return result;
    }

    private boolean isSensitiveHeader(@Nullable String name) {
        if (Objects.isNull(name)) {
            return false;
        }
        var normalized = name.toLowerCase(Locale.ROOT);
        // Built-in credential headers are always masked, even when mask.enabled=false,
        // so disabling field masking can never leak Authorization/Cookie/etc.
        if (defaultHeaderMasker.isSensitive(normalized)) {
            return true;
        }
        return maskingEnabled && masker.isSensitive(normalized);
    }

    private List<String> maskAll(@Nullable List<String> values) {
        if (Objects.isNull(values) || values.isEmpty()) {
            return List.of(SensitiveValueMasker.MASK);
        }
        var masked = new ArrayList<String>(values.size());
        for (var i = 0; i < values.size(); i++) {
            masked.add(SensitiveValueMasker.MASK);
        }
        return masked;
    }
}
