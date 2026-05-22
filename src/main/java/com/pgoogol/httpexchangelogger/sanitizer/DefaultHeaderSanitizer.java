package com.pgoogol.httpexchangelogger.sanitizer;

import com.pgoogol.httpexchangelogger.autoconfigure.HttpExchangeLoggerProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
                                  HttpExchangeLoggerProperties.Mask maskProperties) {
        this.masker = masker;
        this.defaultHeaderMasker = new SensitiveValueMasker(DEFAULT_SENSITIVE_HEADERS);
        this.maskingEnabled = maskProperties != null && maskProperties.isEnabled();
    }

    @Override
    public Map<String, List<String>> sanitize(Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return headers;
        }

        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String name = entry.getKey();
            List<String> values = entry.getValue();

            if (isSensitiveHeader(name)) {
                result.put(name, maskAll(values));
            } else {
                result.put(name, values == null ? Collections.emptyList() : new ArrayList<>(values));
            }
        }
        return result;
    }

    private boolean isSensitiveHeader(String name) {
        if (name == null) {
            return false;
        }
        String normalized = name.toLowerCase(Locale.ROOT);
        // Built-in credential headers are always masked, even when mask.enabled=false,
        // so disabling field masking can never leak Authorization/Cookie/etc.
        if (defaultHeaderMasker.isSensitive(normalized)) {
            return true;
        }
        return maskingEnabled && masker.isSensitive(normalized);
    }

    private List<String> maskAll(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of(SensitiveValueMasker.MASK);
        }
        List<String> masked = new ArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) {
            masked.add(SensitiveValueMasker.MASK);
        }
        return masked;
    }
}
