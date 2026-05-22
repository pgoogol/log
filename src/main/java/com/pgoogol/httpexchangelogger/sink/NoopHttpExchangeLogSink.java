package com.pgoogol.httpexchangelogger.sink;

import com.pgoogol.httpexchangelogger.model.HttpExchangeLogEvent;
import org.jspecify.annotations.NonNull;

public class NoopHttpExchangeLogSink implements HttpExchangeLogSink {

    @Override
    public void log(@NonNull HttpExchangeLogEvent event) {
    }
}
