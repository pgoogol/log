package com.yourcompany.httpexchangelogger.integration;

import com.yourcompany.httpexchangelogger.model.HttpExchangeLogEvent;
import com.yourcompany.httpexchangelogger.sink.HttpExchangeLogSink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RecordingHttpExchangeLogSink implements HttpExchangeLogSink {

    private final List<HttpExchangeLogEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void log(HttpExchangeLogEvent event) {
        events.add(event);
    }

    public List<HttpExchangeLogEvent> getEvents() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    public HttpExchangeLogEvent last() {
        if (events.isEmpty()) {
            throw new IllegalStateException("No events recorded");
        }
        return events.get(events.size() - 1);
    }

    public void clear() {
        events.clear();
    }
}
