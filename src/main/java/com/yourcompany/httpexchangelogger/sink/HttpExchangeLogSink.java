package com.yourcompany.httpexchangelogger.sink;

import com.yourcompany.httpexchangelogger.model.HttpExchangeLogEvent;

public interface HttpExchangeLogSink {

    void log(HttpExchangeLogEvent event);
}
