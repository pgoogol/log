package com.pgoogol.httpexchangelogger.sink;

import com.pgoogol.httpexchangelogger.model.HttpExchangeLogEvent;
import com.pgoogol.httpexchangelogger.serialization.HttpExchangeLogEventJsonWriter;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class ConsoleHttpExchangeLogSink implements HttpExchangeLogSink {

    public static final String LOGGER_NAME = "http.exchange.logger";

    private final Logger logger;
    private final HttpExchangeLogEventJsonWriter jsonWriter;

    public ConsoleHttpExchangeLogSink(HttpExchangeLogEventJsonWriter jsonWriter) {
        this(LoggerFactory.getLogger(LOGGER_NAME), jsonWriter);
    }

    ConsoleHttpExchangeLogSink(Logger logger, HttpExchangeLogEventJsonWriter jsonWriter) {
        this.logger = logger;
        this.jsonWriter = jsonWriter;
    }

    @Override
    public void log(@NonNull HttpExchangeLogEvent event) {
        if (Objects.isNull(event)) {
            return;
        }
        if (!logger.isInfoEnabled()) {
            return;
        }
        var json = jsonWriter.write(event);
        logger.info(json);
    }
}
