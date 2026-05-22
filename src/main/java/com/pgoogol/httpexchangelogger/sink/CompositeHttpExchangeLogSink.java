package com.pgoogol.httpexchangelogger.sink;

import com.pgoogol.httpexchangelogger.model.HttpExchangeLogEvent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class CompositeHttpExchangeLogSink implements HttpExchangeLogSink {

    private static final Logger LOG = LoggerFactory.getLogger(CompositeHttpExchangeLogSink.class);

    private final List<HttpExchangeLogSink> delegates;

    public CompositeHttpExchangeLogSink(@Nullable List<HttpExchangeLogSink> delegates) {
        this.delegates = List.copyOf(Objects.requireNonNullElseGet(delegates, List::of));
    }

    @Override
    public void log(@NonNull HttpExchangeLogEvent event) {
        for (var delegate : delegates) {
            try {
                delegate.log(event);
            } catch (RuntimeException ex) {
                LOG.warn("HTTP exchange log sink {} failed to emit event", delegate.getClass().getName(), ex);
            }
        }
    }

    public List<HttpExchangeLogSink> getDelegates() {
        return delegates;
    }
}
