package com.pgoogol.httpexchangelogger.sink;

import com.pgoogol.httpexchangelogger.model.HttpExchangeLogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Decouples event emission from request handling: {@link #log(HttpExchangeLogEvent)}
 * enqueues the event and returns immediately, a single background thread delivers
 * queued events to the delegate sink.
 *
 * <p>The request thread is never blocked: when the queue is full the event is
 * dropped and counted. On {@link #close()} the queue is drained to the delegate
 * before the worker stops (bounded by the configured shutdown timeout).
 */
public class AsyncHttpExchangeLogSink implements HttpExchangeLogSink, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncHttpExchangeLogSink.class);

    private static final long POLL_INTERVAL_MS = 100;
    private static final int DROP_LOG_INTERVAL = 100;

    private final HttpExchangeLogSink delegate;
    private final BlockingQueue<HttpExchangeLogEvent> queue;
    private final long shutdownTimeoutMs;
    private final Thread worker;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicLong droppedCount = new AtomicLong();

    private volatile boolean running = true;

    public AsyncHttpExchangeLogSink(HttpExchangeLogSink delegate, int queueCapacity, long shutdownTimeoutMs) {

        this.delegate = delegate;
        this.queue = new ArrayBlockingQueue<>(Math.max(1, queueCapacity));
        this.shutdownTimeoutMs = shutdownTimeoutMs;
        this.worker = new Thread(this::drainLoop, "http-exchange-logger-async");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    @Override
    public void log(HttpExchangeLogEvent event) {

        if (Objects.isNull(event) || !running) {

            return;
        }
        boolean accepted = queue.offer(event);
        if (!accepted) {

            long dropped = droppedCount.incrementAndGet();
            if (dropped % DROP_LOG_INTERVAL == 1) {

                LOG.warn("Async HTTP exchange log queue full; dropped {} events so far", dropped);
            }
        }
    }

    @Override
    public void close() {

        if (!closed.compareAndSet(false, true)) {

            return;
        }
        running = false;
        try {

            worker.join(shutdownTimeoutMs);
        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();
        }
        if (worker.isAlive()) {

            worker.interrupt();
            LOG.warn("Async HTTP exchange log worker did not finish within {} ms; up to {} events may be lost",
                    shutdownTimeoutMs, queue.size());
        }
    }

    public long getDroppedCount() {

        return droppedCount.get();
    }

    private void drainLoop() {

        try {

            while (true) {

                HttpExchangeLogEvent event = queue.poll(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
                if (Objects.nonNull(event)) {

                    emit(event);
                    continue;
                }
                if (!running) {

                    return;
                }
            }
        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();
        }
    }

    private void emit(HttpExchangeLogEvent event) {

        try {

            delegate.log(event);
        } catch (RuntimeException ex) {

            LOG.warn("Async HTTP exchange log delegate failed to emit event", ex);
        }
    }

}
