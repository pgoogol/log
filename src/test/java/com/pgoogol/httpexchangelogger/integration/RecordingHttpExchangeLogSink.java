package com.pgoogol.httpexchangelogger.integration;

import com.pgoogol.httpexchangelogger.model.HttpExchangeLogEvent;
import com.pgoogol.httpexchangelogger.sink.HttpExchangeLogSink;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RecordingHttpExchangeLogSink implements HttpExchangeLogSink {

    private final List<HttpExchangeLogEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void log(HttpExchangeLogEvent event) {

        events.add(event);
    }

    public List<HttpExchangeLogEvent> getEvents() {

        return List.copyOf(events);
    }

    public HttpExchangeLogEvent last() {

        if (events.isEmpty()) {

            throw new IllegalStateException("No events recorded");
        }
        return events.getLast();
    }

    public void clear() {

        events.clear();
    }

}
