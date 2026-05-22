package com.pgoogol.httpexchangelogger.sanitizer;

import com.pgoogol.httpexchangelogger.autoconfigure.HttpExchangeLoggerProperties;
import com.pgoogol.httpexchangelogger.support.ContentTypeMatcher;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JsonBodySanitizer implements BodySanitizer {

    public static final String UNPARSEABLE_PLACEHOLDER =
            "[not logged: body could not be parsed for masking]";

    private final ObjectMapper objectMapper;
    private final SensitiveValueMasker masker;
    private final boolean maskingEnabled;
    private final boolean hasSensitiveFields;

    public JsonBodySanitizer(ObjectMapper objectMapper,
                             SensitiveValueMasker masker,
                             HttpExchangeLoggerProperties.Mask maskProperties) {
        this.objectMapper = objectMapper;
        this.masker = masker;
        this.maskingEnabled = maskProperties != null && maskProperties.isEnabled();
        this.hasSensitiveFields = maskProperties != null
                && maskProperties.getFields() != null
                && !maskProperties.getFields().isEmpty();
    }

    @Override
    public String sanitize(String body, String contentType) {
        if (body == null || body.isEmpty()) {
            return body;
        }
        if (!maskingEnabled) {
            return body;
        }
        if (!ContentTypeMatcher.isJson(contentType)) {
            return body;
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode sanitized = mask(root);
            return objectMapper.writeValueAsString(sanitized);
        } catch (Exception ex) {
            // Fail closed: masking was requested but the body could not be parsed
            // (malformed or truncated JSON). Never echo the raw body — it may carry secrets.
            return hasSensitiveFields ? UNPARSEABLE_PLACEHOLDER : body;
        }
    }

    private JsonNode mask(JsonNode node) {
        if (node == null || node.isNull()) {
            return node;
        }
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            List<String> names = new ArrayList<>();
            for (Map.Entry<String, JsonNode> entry : object.properties()) {
                names.add(entry.getKey());
            }
            for (String name : names) {
                if (masker.isSensitive(name)) {
                    object.put(name, SensitiveValueMasker.MASK);
                } else {
                    JsonNode value = object.get(name);
                    if (value != null && (value.isObject() || value.isArray())) {
                        mask(value);
                    }
                }
            }
            return object;
        }
        if (node.isArray()) {
            ArrayNode array = (ArrayNode) node;
            for (int i = 0; i < array.size(); i++) {
                JsonNode item = array.get(i);
                if (item.isObject() || item.isArray()) {
                    mask(item);
                }
            }
            return array;
        }
        return node;
    }
}
