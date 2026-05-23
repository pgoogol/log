package com.pgoogol.httpexchangelogger.sink;

import com.pgoogol.httpexchangelogger.model.HttpExchangeLogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class CompositeHttpExchangeLogSink implements HttpExchangeLogSink {

    private static final Logger LOG = LoggerFactory.getLogger(CompositeHttpExchangeLogSink.class);

    private final List<HttpExchangeLogSink> delegates;

    public CompositeHttpExchangeLogSink(List<HttpExchangeLogSink> delegates) {

        if (Objects.isNull(delegates)) {

            this.delegates = List.of();
        } else {

            this.delegates = List.copyOf(delegates);
        }
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
