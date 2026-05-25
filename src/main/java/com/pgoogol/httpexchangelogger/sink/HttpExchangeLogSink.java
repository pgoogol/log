package com.pgoogol.httpexchangelogger.sink;

import com.pgoogol.httpexchangelogger.model.HttpExchangeLogEvent;

public interface HttpExchangeLogSink {

    void log(HttpExchangeLogEvent event);

}
