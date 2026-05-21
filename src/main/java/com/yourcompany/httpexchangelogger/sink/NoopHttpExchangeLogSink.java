package com.yourcompany.httpexchangelogger.sink;

import com.yourcompany.httpexchangelogger.model.HttpExchangeLogEvent;

public class NoopHttpExchangeLogSink implements HttpExchangeLogSink {

    @Override
    public void log(HttpExchangeLogEvent event) {
    }
}
