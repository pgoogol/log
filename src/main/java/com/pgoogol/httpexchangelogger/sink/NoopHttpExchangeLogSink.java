package com.pgoogol.httpexchangelogger.sink;

import com.pgoogol.httpexchangelogger.model.HttpExchangeLogEvent;

public class NoopHttpExchangeLogSink implements HttpExchangeLogSink {

    @Override
    public void log(HttpExchangeLogEvent event) {
    }
}
