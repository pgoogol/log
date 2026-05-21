package com.yourcompany.httpexchangelogger.sink;

import com.yourcompany.httpexchangelogger.model.HttpExchangeLogEvent;
import com.yourcompany.httpexchangelogger.serialization.HttpExchangeLogEventJsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public void log(HttpExchangeLogEvent event) {
        if (event == null) {
            return;
        }
        if (!logger.isInfoEnabled()) {
            return;
        }
        String json = jsonWriter.write(event);
        logger.info(json);
    }
}
