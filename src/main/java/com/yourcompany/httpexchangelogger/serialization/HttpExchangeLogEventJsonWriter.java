package com.yourcompany.httpexchangelogger.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourcompany.httpexchangelogger.model.HttpExchangeLogEvent;

public class HttpExchangeLogEventJsonWriter {

    private final ObjectMapper objectMapper;

    public HttpExchangeLogEventJsonWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(HttpExchangeLogEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            return "{\"type\":\"http_exchange\",\"serializationError\":\""
                    + escape(ex.getOriginalMessage()) + "\"}";
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
