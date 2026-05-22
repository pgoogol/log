package com.pgoogol.httpexchangelogger.sanitizer;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SensitiveValueMasker {

    public static final String MASK = "***";

    private final Set<String> normalizedFields;

    public SensitiveValueMasker(List<String> fields) {
        this.normalizedFields = new HashSet<>();
        if (fields != null) {
            for (String field : fields) {
                if (field != null) {
                    normalizedFields.add(normalize(field));
                }
            }
        }
    }

    public boolean isSensitive(String name) {
        if (name == null) {
            return false;
        }
        return normalizedFields.contains(normalize(name));
    }

    public String mask(String value) {
        if (value == null) {
            return null;
        }
        return MASK;
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
