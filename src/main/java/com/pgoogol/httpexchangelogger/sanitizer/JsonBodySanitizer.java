package com.pgoogol.httpexchangelogger.sanitizer;

import com.pgoogol.httpexchangelogger.autoconfigure.HttpExchangeLoggerProperties;
import com.pgoogol.httpexchangelogger.support.ContentTypeMatcher;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Objects;

public class JsonBodySanitizer implements BodySanitizer {

    public static final String UNPARSEABLE_PLACEHOLDER =
            "[not logged: body could not be parsed for masking]";

    private final ObjectMapper objectMapper;
    private final SensitiveValueMasker masker;
    private final boolean maskingEnabled;
    private final boolean hasSensitiveFields;

    public JsonBodySanitizer(ObjectMapper objectMapper,
                             SensitiveValueMasker masker,
                             HttpExchangeLoggerProperties.@Nullable Mask maskProperties) {
        this.objectMapper = objectMapper;
        this.masker = masker;
        this.maskingEnabled = Objects.nonNull(maskProperties) && maskProperties.isEnabled();
        this.hasSensitiveFields = Objects.nonNull(maskProperties)
                && Objects.nonNull(maskProperties.getFields())
                && !maskProperties.getFields().isEmpty();
    }

    @Override
    public @Nullable String sanitize(@Nullable String body, @Nullable String contentType) {
        if (Objects.isNull(body) || body.isEmpty()) {
            return body;
        }
        if (!maskingEnabled) {
            return body;
        }
        if (!ContentTypeMatcher.isJson(contentType)) {
            return body;
        }

        try {
            var root = objectMapper.readTree(body);
            var sanitized = mask(root);
            return objectMapper.writeValueAsString(sanitized);
        } catch (Exception ex) {
            // Fail closed: masking was requested but the body could not be parsed
            // (malformed or truncated JSON). Never echo the raw body — it may carry secrets.
            return hasSensitiveFields ? UNPARSEABLE_PLACEHOLDER : body;
        }
    }

    private JsonNode mask(@Nullable JsonNode node) {
        if (Objects.isNull(node) || node.isNull()) {
            return node;
        }
        if (node.isObject()) {
            var object = (ObjectNode) node;
            var names = new ArrayList<String>();
            for (var entry : object.properties()) {
                names.add(entry.getKey());
            }
            for (var name : names) {
                if (masker.isSensitive(name)) {
                    object.put(name, SensitiveValueMasker.MASK);
                } else {
                    var value = object.get(name);
                    if (Objects.nonNull(value) && (value.isObject() || value.isArray())) {
                        mask(value);
                    }
                }
            }
            return object;
        }
        if (node.isArray()) {
            var array = (ArrayNode) node;
            for (var i = 0; i < array.size(); i++) {
                var item = array.get(i);
                if (item.isObject() || item.isArray()) {
                    mask(item);
                }
            }
            return array;
        }
        return node;
    }
}
