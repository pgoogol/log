package com.pgoogol.httpexchangelogger.sink;

import com.pgoogol.httpexchangelogger.model.HttpExchangeLogEvent;
import org.jspecify.annotations.NonNull;

public interface HttpExchangeLogSink {

    void log(@NonNull HttpExchangeLogEvent event);
}
