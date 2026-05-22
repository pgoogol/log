package com.pgoogol.httpexchangelogger.sanitizer;

import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class SensitiveValueMasker {

    public static final String MASK = "***";

    private final Set<String> normalizedFields;

    public SensitiveValueMasker(@Nullable List<String> fields) {
        this.normalizedFields = new HashSet<>();
        if (Objects.nonNull(fields)) {
            for (var field : fields) {
                if (Objects.nonNull(field)) {
                    normalizedFields.add(normalize(field));
                }
            }
        }
    }

    public boolean isSensitive(@Nullable String name) {
        if (Objects.isNull(name)) {
            return false;
        }
        return normalizedFields.contains(normalize(name));
    }

    public @Nullable String mask(@Nullable String value) {
        if (Objects.isNull(value)) {
            return null;
        }
        return MASK;
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
