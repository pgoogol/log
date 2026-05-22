package com.pgoogol.httpexchangelogger.sanitizer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pgoogol.httpexchangelogger.autoconfigure.HttpExchangeLoggerProperties;
import com.pgoogol.httpexchangelogger.support.ContentTypeMatcher;

import java.util.Iterator;
import java.util.Map;

public class JsonBodySanitizer implements BodySanitizer {

    private final ObjectMapper objectMapper;
    private final SensitiveValueMasker masker;
    private final boolean maskingEnabled;

    public JsonBodySanitizer(ObjectMapper objectMapper,
                             SensitiveValueMasker masker,
                             HttpExchangeLoggerProperties.Mask maskProperties) {
        this.objectMapper = objectMapper;
        this.masker = masker;
        this.maskingEnabled = maskProperties != null && maskProperties.isEnabled();
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
            return body;
        }
    }

    private JsonNode mask(JsonNode node) {
        if (node == null || node.isNull()) {
            return node;
        }
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String fieldName = entry.getKey();
                JsonNode value = entry.getValue();
                if (masker.isSensitive(fieldName)) {
                    object.put(fieldName, SensitiveValueMasker.MASK);
                } else if (value.isObject() || value.isArray()) {
                    mask(value);
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
