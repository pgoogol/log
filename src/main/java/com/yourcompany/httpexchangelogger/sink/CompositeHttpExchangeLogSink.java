package com.yourcompany.httpexchangelogger.sink;

import com.yourcompany.httpexchangelogger.model.HttpExchangeLogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CompositeHttpExchangeLogSink implements HttpExchangeLogSink {

    private static final Logger LOG = LoggerFactory.getLogger(CompositeHttpExchangeLogSink.class);

    private final List<HttpExchangeLogSink> delegates;

    public CompositeHttpExchangeLogSink(List<HttpExchangeLogSink> delegates) {
        this.delegates = delegates == null ? List.of() : List.copyOf(delegates);
    }

    @Override
    public void log(HttpExchangeLogEvent event) {
        for (HttpExchangeLogSink delegate : delegates) {
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
